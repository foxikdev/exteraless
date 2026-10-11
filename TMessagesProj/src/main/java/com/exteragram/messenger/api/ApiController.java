package com.exteragram.messenger.api;

import android.content.SharedPreferences;

import com.exteragram.messenger.ExteraConfig;
import com.exteragram.messenger.api.db.ProfileStore;
import com.exteragram.messenger.api.dto.ProfileDTO;
import com.exteragram.messenger.api.network.ApiClient;
import com.exteragram.messenger.badges.BadgesController;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import org.telegram.messenger.FileLog;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/** Profile API controller with the public names and sync keys used by exteraGram. */
public final class ApiController {
    private static final String SYNC_TIMESTAMP_HEADER = "X-Sync-Timestamp";
    private static final String PROFILES_SYNC_TIME_KEY = "lastProfilesSyncTime";
    private static final String BOOSTY_SYNC_TIME_KEY = "lastBoostySyncTime";
    private static final String SYNC_FAILURE_TIME_KEY = "lastSyncFailureTime";
    private static final long PROFILES_SYNC_INTERVAL = TimeUnit.MINUTES.toMillis(10);
    private static final long BOOSTY_SYNC_INTERVAL = TimeUnit.MINUTES.toMillis(40);
    private static final long FAILURE_BACKOFF = TimeUnit.MINUTES.toMillis(5);
    private static final String PROFILES_ETAG_KEY = "profilesEtag";
    private static final String SYNC_WATERMARK_KEY = "lastSyncTimestamp";
    private static final Type PROFILES_TYPE = new TypeToken<List<ProfileDTO>>() { }.getType();

    public static final ApiController INSTANCE = new ApiController();
    private static final ExecutorService scope = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "extera-api-sync");
        thread.setDaemon(true);
        return thread;
    });
    private static final AtomicBoolean started = new AtomicBoolean(false);
    private final Object syncMutex = new Object();
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    private ApiController() {
    }

    public static void init() {
        if (started.compareAndSet(false, true)) {
            sync();
            scheduleWorker();
        }
    }

    public static void sync() {
        sync(ExteraConfig.getPreferences(), false);
    }

    public static void sync(SharedPreferences prefs) {
        sync(prefs, false);
    }

    public static void sync(SharedPreferences prefs, boolean force) {
        if (prefs == null || !ApiClient.INSTANCE.getRequestsEnabled$TMessagesProj()) {
            return;
        }
        // This check happens before any request/client work: an unset URL disables the API completely.
        if (empty(ExteraConfig.getApiBaseUrl())) {
            return;
        }
        long now = System.currentTimeMillis();
        if (!force && elapsedSince(now, prefs.getLong(SYNC_FAILURE_TIME_KEY, 0)) < FAILURE_BACKOFF) {
            return;
        }
        boolean profilesDue = elapsedSince(now, prefs.getLong(PROFILES_SYNC_TIME_KEY, 0)) >= PROFILES_SYNC_INTERVAL;
        if (!force && !profilesDue) {
            return;
        }
        scope.execute(() -> INSTANCE.performSync(prefs, force));
    }

    public static void resetSyncState() {
        resetSyncState(ExteraConfig.getPreferences());
    }

    public static void resetSyncState(SharedPreferences prefs) {
        if (prefs != null) {
            prefs.edit().remove(SYNC_WATERMARK_KEY).remove(PROFILES_ETAG_KEY)
                    .remove(PROFILES_SYNC_TIME_KEY).remove(BOOSTY_SYNC_TIME_KEY)
                    .remove(SYNC_FAILURE_TIME_KEY).apply();
        }
    }

    public static void scheduleWorker() {
        // The original worker is periodic; the timestamp gate above preserves the same 10-minute behavior.
    }

    private void performSync(SharedPreferences prefs, boolean force) {
        synchronized (syncMutex) {
            String baseUrl = ExteraConfig.getApiBaseUrl();
            String watermark = prefs.getString(SYNC_WATERMARK_KEY, null);
            String etag = prefs.getString(PROFILES_ETAG_KEY, null);
            boolean full = empty(watermark);
            String url = profilesUrl(baseUrl, full ? null : watermark, full);
            if (url == null) {
                return;
            }
            Request.Builder builder = new Request.Builder().url(url).get();
            if (!empty(etag)) {
                builder.header("If-None-Match", etag);
            }
            Request request = builder.build();
            try (Response response = client.newCall(request).execute()) {
                if (response.code() == 304) {
                    markSuccess(prefs, response, etag);
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    markFailure(prefs);
                    return;
                }
                List<ProfileDTO> profiles = parse(response.body().string());
                if (profiles == null) {
                    markFailure(prefs);
                    return;
                }
                if (full) {
                    ProfileStore.INSTANCE.replaceAll(profiles);
                } else {
                    mergeUpdates(profiles);
                }
                BadgesController.INSTANCE.getApiBadgeSource().loadToCache();
                markSuccess(prefs, response, response.header("ETag"));
            } catch (Exception e) {
                markFailure(prefs);
                FileLog.e("ApiController: profile sync failed", e);
            }
        }
    }

    private static void mergeUpdates(List<ProfileDTO> updates) {
        List<ProfileDTO> stored = ProfileStore.INSTANCE.getAll();
        java.util.Map<Long, ProfileDTO> merged = new java.util.LinkedHashMap<>();
        for (ProfileDTO profile : stored) {
            if (profile != null) {
                merged.put(profile.getId(), profile);
            }
        }
        for (ProfileDTO profile : updates) {
            if (profile == null) {
                continue;
            }
            if (Boolean.TRUE.equals(profile.getDeleted())) {
                merged.remove(profile.getId());
            } else {
                merged.put(profile.getId(), profile);
            }
        }
        ProfileStore.INSTANCE.replaceAll(new ArrayList<>(merged.values()));
    }

    private static void markSuccess(SharedPreferences prefs, Response response, String responseEtag) {
        String watermark = response.header(SYNC_TIMESTAMP_HEADER);
        if (empty(watermark)) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            watermark = format.format(new Date());
        }
        SharedPreferences.Editor editor = prefs.edit().putString(SYNC_WATERMARK_KEY, watermark)
                .putLong(PROFILES_SYNC_TIME_KEY, System.currentTimeMillis()).remove(SYNC_FAILURE_TIME_KEY);
        if (responseEtag == null) {
            editor.remove(PROFILES_ETAG_KEY);
        } else {
            editor.putString(PROFILES_ETAG_KEY, responseEtag);
        }
        editor.apply();
    }

    private static void markFailure(SharedPreferences prefs) {
        prefs.edit().putLong(SYNC_FAILURE_TIME_KEY, System.currentTimeMillis()).apply();
    }

    private static List<ProfileDTO> parse(String body) {
        JsonElement root = JsonParser.parseString(body);
        if (root.isJsonArray()) {
            return ExteraConfig.getGSON().fromJson(root, PROFILES_TYPE);
        }
        if (root.isJsonObject()) {
            JsonObject object = root.getAsJsonObject();
            JsonElement values = object.get("profiles");
            if (values == null) {
                values = object.get("data");
            }
            return values != null && values.isJsonArray()
                    ? ExteraConfig.getGSON().fromJson(values, PROFILES_TYPE) : null;
        }
        return null;
    }

    private static String profilesUrl(String configured, String since, boolean full) {
        if (empty(configured)) {
            return null;
        }
        try {
            URI uri = URI.create(configured.trim());
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null) {
                return null;
            }
            String path = uri.getPath() == null ? "" : uri.getPath().replaceFirst("/+$", "");
            if (!path.endsWith("/profiles") && !path.endsWith("/profiles/updates")) {
                path += "/profiles";
            }
            if (!full) {
                path += "/updates";
            }
            URI endpoint = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), path, null, null);
            String result = endpoint.toString();
            return full || empty(since) ? result : result + "?since=" + URLEncoder.encode(since, StandardCharsets.UTF_8.name());
        } catch (IllegalArgumentException | URISyntaxException | IOException e) {
            return null;
        }
    }

    private static boolean empty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static long elapsedSince(long now, long timestamp) {
        return timestamp <= 0 || timestamp > now ? Long.MAX_VALUE : now - timestamp;
    }
}
