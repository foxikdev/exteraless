package com.exteragram.messenger.api.db;

import com.exteragram.messenger.api.dto.NowPlayingInfoDTO;

import java.util.List;
import java.util.function.Consumer;

import kotlin.Unit;
import kotlin.coroutines.Continuation;

/** Compatibility helper retaining the profile database method names used by plugins. */
public final class DatabaseHelper {
    public static final DatabaseHelper INSTANCE = new DatabaseHelper();

    private DatabaseHelper() {
    }

    public Object insertProfiles(List<com.exteragram.messenger.api.dto.ProfileDTO> profiles,
                                 Continuation<? super Boolean> continuation) {
        ProfileStore.INSTANCE.insertAll(profiles);
        return Boolean.TRUE;
    }

    public Object deleteProfiles(List<Long> ids, Continuation<? super Boolean> continuation) {
        ProfileStore.INSTANCE.deleteProfiles(ids);
        return Boolean.TRUE;
    }

    public static void getNowPlaying(long id, Consumer<NowPlayingInfoDTO> callback) {
        if (callback != null) {
            ProfileDTOHolder.accept(id, callback);
        }
    }

    public static void updateNowPlaying(long id, NowPlayingInfoDTO nowPlaying, Consumer<Integer> callback) {
        ProfileStore.INSTANCE.updateNowPlaying(id, nowPlaying);
        if (callback != null) {
            callback.accept(Integer.valueOf(1));
        }
    }

    private static final class ProfileDTOHolder {
        private static void accept(long id, Consumer<NowPlayingInfoDTO> callback) {
            com.exteragram.messenger.api.dto.ProfileDTO profile = ProfileStore.INSTANCE.getById(id);
            callback.accept(profile == null ? null : profile.getNowPlaying());
        }
    }
}
