package com.exteragram.messenger.api.db;

import com.exteragram.messenger.ExteraConfig;
import com.exteragram.messenger.api.dto.BadgeDTO;
import com.exteragram.messenger.api.dto.NowPlayingInfoDTO;
import com.exteragram.messenger.api.dto.ProfileDTO;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Persistent profile store used when the full Room API module is unavailable in this fork. */
public final class ProfileStore implements ProfileDao {
    public static final ProfileStore INSTANCE = new ProfileStore();
    private static final String KEY = "apiProfiles";
    private static final Type LIST_TYPE = new TypeToken<List<ProfileDTO>>() { }.getType();

    private final Object lock = new Object();
    private final LinkedHashMap<Long, ProfileDTO> profiles = new LinkedHashMap<>();
    private boolean loaded;

    private ProfileStore() {
    }

    private void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        String json = ExteraConfig.getPreferences().getString(KEY, null);
        if (json == null || json.trim().isEmpty()) {
            return;
        }
        try {
            List<ProfileDTO> values = ExteraConfig.getGSON().fromJson(json, LIST_TYPE);
            if (values != null) {
                for (ProfileDTO profile : values) {
                    if (profile != null) {
                        profiles.put(profile.getId(), profile);
                    }
                }
            }
        } catch (RuntimeException ignored) {
            profiles.clear();
        }
    }

    private void persist() {
        ExteraConfig.getPreferences().edit()
                .putString(KEY, ExteraConfig.getGSON().toJson(new ArrayList<>(profiles.values())))
                .apply();
    }

    @Override
    public void insertAll(List<ProfileDTO> values) {
        synchronized (lock) {
            ensureLoaded();
            if (values != null) {
                for (ProfileDTO profile : values) {
                    if (profile != null) {
                        profiles.put(profile.getId(), profile);
                    }
                }
                persist();
            }
        }
    }

    public void replaceAll(List<ProfileDTO> values) {
        synchronized (lock) {
            ensureLoaded();
            profiles.clear();
            if (values != null) {
                for (ProfileDTO profile : values) {
                    if (profile != null && !Boolean.TRUE.equals(profile.getDeleted())) {
                        profiles.put(profile.getId(), profile);
                    }
                }
            }
            persist();
        }
    }

    @Override
    public List<ProfileDTO> getAll() {
        synchronized (lock) {
            ensureLoaded();
            return new ArrayList<>(profiles.values());
        }
    }

    @Override
    public ProfileDTO getById(long id) {
        synchronized (lock) {
            ensureLoaded();
            return profiles.get(id);
        }
    }

    @Override
    public void deleteProfiles(List<Long> ids) {
        synchronized (lock) {
            ensureLoaded();
            if (ids != null) {
                for (Long id : ids) {
                    if (id != null) {
                        profiles.remove(id);
                    }
                }
                persist();
            }
        }
    }

    @Override
    public void updateBadge(long id, BadgeDTO badge) {
        synchronized (lock) {
            ensureLoaded();
            ProfileDTO profile = profiles.get(id);
            if (profile != null) {
                profiles.put(id, profile.copy(profile.getId(), profile.getType(), profile.getStatus(), badge,
                        profile.getNowPlaying(), profile.getDeleted(), profile.getCanChangeBadge()));
                persist();
            }
        }
    }

    @Override
    public void updateNowPlaying(long id, NowPlayingInfoDTO nowPlaying) {
        synchronized (lock) {
            ensureLoaded();
            ProfileDTO profile = profiles.get(id);
            if (profile != null) {
                profiles.put(id, profile.copy(profile.getId(), profile.getType(), profile.getStatus(), profile.getBadge(),
                        nowPlaying, profile.getDeleted(), profile.getCanChangeBadge()));
                persist();
            }
        }
    }
}
