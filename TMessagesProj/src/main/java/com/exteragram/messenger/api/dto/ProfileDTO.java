package com.exteragram.messenger.api.dto;

import com.exteragram.messenger.api.model.ProfileStatus;
import com.exteragram.messenger.api.model.ProfileType;

import java.util.Objects;

/** The same profile model exposed by exteraGram's API layer. */
public final class ProfileDTO {
    private final long id;
    private final ProfileType type;
    private final ProfileStatus status;
    private final BadgeDTO badge;
    private final NowPlayingInfoDTO nowPlaying;
    private final Boolean deleted;
    private final Boolean canChangeBadge;

    public ProfileDTO(long id, ProfileType type, ProfileStatus status, BadgeDTO badge,
                      NowPlayingInfoDTO nowPlaying, Boolean deleted, Boolean canChangeBadge) {
        this.id = id;
        this.type = type;
        this.status = status;
        this.badge = badge;
        this.nowPlaying = nowPlaying;
        this.deleted = deleted;
        this.canChangeBadge = canChangeBadge;
    }

    public long getId() { return id; }
    public ProfileType getType() { return type; }
    public ProfileStatus getStatus() { return status; }
    public BadgeDTO getBadge() { return badge; }
    public NowPlayingInfoDTO getNowPlaying() { return nowPlaying; }
    public Boolean getDeleted() { return deleted; }
    public Boolean getCanChangeBadge() { return canChangeBadge; }

    public ProfileDTO copy(long id, ProfileType type, ProfileStatus status, BadgeDTO badge,
                           NowPlayingInfoDTO nowPlaying, Boolean deleted, Boolean canChangeBadge) {
        return new ProfileDTO(id, type, status, badge, nowPlaying, deleted, canChangeBadge);
    }

    public long component1() { return id; }
    public ProfileType component2() { return type; }
    public ProfileStatus component3() { return status; }
    public BadgeDTO component4() { return badge; }
    public NowPlayingInfoDTO component5() { return nowPlaying; }
    public Boolean component6() { return deleted; }
    public Boolean component7() { return canChangeBadge; }

    public static ProfileDTO copy$default(ProfileDTO value, long id, ProfileType type, ProfileStatus status,
                                          BadgeDTO badge, NowPlayingInfoDTO nowPlaying, Boolean deleted,
                                          Boolean canChangeBadge, int mask, Object ignored) {
        return value.copy((mask & 1) != 0 ? value.id : id,
                (mask & 2) != 0 ? value.type : type,
                (mask & 4) != 0 ? value.status : status,
                (mask & 8) != 0 ? value.badge : badge,
                (mask & 16) != 0 ? value.nowPlaying : nowPlaying,
                (mask & 32) != 0 ? value.deleted : deleted,
                (mask & 64) != 0 ? value.canChangeBadge : canChangeBadge);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ProfileDTO)) return false;
        ProfileDTO value = (ProfileDTO) other;
        return id == value.id && type == value.type && status == value.status
                && Objects.equals(badge, value.badge) && Objects.equals(nowPlaying, value.nowPlaying)
                && Objects.equals(deleted, value.deleted) && Objects.equals(canChangeBadge, value.canChangeBadge);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, status, badge, nowPlaying, deleted, canChangeBadge);
    }

    @Override
    public String toString() {
        return "ProfileDTO(id=" + id + ", type=" + type + ", status=" + status + ", badge=" + badge
                + ", nowPlaying=" + nowPlaying + ", deleted=" + deleted
                + ", canChangeBadge=" + canChangeBadge + ")";
    }
}
