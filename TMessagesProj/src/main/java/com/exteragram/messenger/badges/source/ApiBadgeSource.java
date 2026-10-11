package com.exteragram.messenger.badges.source;

import com.exteragram.messenger.api.dto.BadgeDTO;
import com.exteragram.messenger.api.db.ProfileDao;
import com.exteragram.messenger.api.dto.ProfileDTO;
import com.exteragram.messenger.api.model.ProfileStatus;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

public final class ApiBadgeSource {

    private final ConcurrentHashMap<Long, BadgeInfo> cache = new ConcurrentHashMap<>();
    private final ProfileDao profileDao;

    public ApiBadgeSource() {
        this(null);
    }

    public ApiBadgeSource(ProfileDao profileDao) {
        this.profileDao = profileDao;
    }

    public BadgeDTO getBadge(long id, boolean isUser) {
        BadgeInfo info = cache.get(Long.valueOf(id));
        return info == null ? null : info.getBadge();
    }

    public boolean isDeveloper(long id) {
        BadgeInfo info = cache.get(Long.valueOf(id));
        return info != null && info.getStatus() == ProfileStatus.DEVELOPER;
    }

    public boolean canChangeBadge(long id) {
        BadgeInfo info = cache.get(Long.valueOf(id));
        return (info != null && info.getCanChangeBadge()) || isDeveloper(id);
    }

    public BadgeInfo getInfo(long id) {
        return cache.get(Long.valueOf(id));
    }

    public void put(long id, BadgeInfo info) {
        if (info == null) {
            cache.remove(Long.valueOf(id));
        } else {
            cache.put(Long.valueOf(id), info);
        }
    }

    /** Replaces the snapshot received from the remote API atomically. */
    public synchronized void replaceAll(Map<Long, BadgeInfo> values) {
        cache.clear();
        if (values != null && !values.isEmpty()) {
            cache.putAll(values);
        }
    }

    public void clear() {
        cache.clear();
    }

    /** Loads the persisted profile snapshot, matching the original API source contract. */
    public void loadToCache() {
        if (profileDao == null) {
            return;
        }
        List<ProfileDTO> profiles = profileDao.getAll();
        if (profiles == null) {
            return;
        }
        Map<Long, BadgeInfo> values = new java.util.HashMap<>();
        for (ProfileDTO profile : profiles) {
            if (profile != null && !Boolean.TRUE.equals(profile.getDeleted())) {
                values.put(profile.getId(), new BadgeInfo(profile.getBadge(), profile.getStatus(),
                        Boolean.TRUE.equals(profile.getCanChangeBadge())));
            }
        }
        replaceAll(values);
    }

    public void updateLocalBadge(long id, BadgeDTO badge) {
        BadgeInfo current = cache.get(Long.valueOf(id));
        if (current != null) {
            cache.put(Long.valueOf(id), current.copy(badge, current.getStatus(), current.getCanChangeBadge()));
        }
        if (profileDao != null) {
            profileDao.updateBadge(id, badge);
        }
    }

    public Object updateLocalBadge(long id, BadgeDTO badge, Continuation<? super Unit> continuation) {
        updateLocalBadge(id, badge);
        return Unit.INSTANCE;
    }

    public Object loadToCache(Continuation<? super Unit> continuation) {
        loadToCache();
        return Unit.INSTANCE;
    }
}
