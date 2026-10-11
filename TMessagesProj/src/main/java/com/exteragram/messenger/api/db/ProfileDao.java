package com.exteragram.messenger.api.db;

import com.exteragram.messenger.api.dto.BadgeDTO;
import com.exteragram.messenger.api.dto.NowPlayingInfoDTO;
import com.exteragram.messenger.api.dto.ProfileDTO;

import java.util.Collections;
import java.util.List;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

/** Small compatibility surface matching the profile DAO used by exteraGram. */
public interface ProfileDao {
    default void insertAll(List<ProfileDTO> profiles) {
    }

    default List<ProfileDTO> getAll() {
        return Collections.emptyList();
    }

    default ProfileDTO getById(long id) {
        return null;
    }

    default void deleteProfiles(List<Long> ids) {
    }

    default void updateBadge(long id, BadgeDTO badge) {
    }

    default void updateNowPlaying(long id, NowPlayingInfoDTO nowPlaying) {
    }

    // Suspend-method JVM signatures kept for binary compatibility with Kotlin plugins.
    default Object insertAll(List<ProfileDTO> profiles, Continuation<? super Unit> continuation) {
        insertAll(profiles);
        return Unit.INSTANCE;
    }

    default Object getAll(Continuation<? super List<ProfileDTO>> continuation) {
        return getAll();
    }

    default Object getById(long id, Continuation<? super ProfileDTO> continuation) {
        return getById(id);
    }

    default Object deleteProfiles(List<Long> ids, Continuation<? super Integer> continuation) {
        deleteProfiles(ids);
        return Integer.valueOf(1);
    }

    default Object updateBadge(long id, BadgeDTO badge, Continuation<? super Integer> continuation) {
        updateBadge(id, badge);
        return Integer.valueOf(1);
    }

    default Object updateNowPlaying(long id, NowPlayingInfoDTO nowPlaying,
                                    Continuation<? super Integer> continuation) {
        updateNowPlaying(id, nowPlaying);
        return Integer.valueOf(1);
    }
}
