package com.exteragram.messenger.api.dto;

import com.exteragram.messenger.api.model.NowPlayingServiceType;
import java.util.Objects;

/** Profile now-playing payload retained for API/plugin compatibility. */
public final class NowPlayingInfoDTO {
    private final NowPlayingServiceType serviceType;
    private String username;

    public NowPlayingInfoDTO(NowPlayingServiceType serviceType, String username) {
        this.serviceType = serviceType;
        this.username = username;
    }

    public NowPlayingServiceType getServiceType() { return serviceType; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public NowPlayingInfoDTO copy(NowPlayingServiceType serviceType, String username) {
        return new NowPlayingInfoDTO(serviceType, username);
    }
    public NowPlayingServiceType component1() { return serviceType; }
    public String component2() { return username; }
    public static NowPlayingInfoDTO copy$default(NowPlayingInfoDTO value, NowPlayingServiceType serviceType,
                                                  String username, int mask, Object ignored) {
        return value.copy((mask & 1) != 0 ? value.serviceType : serviceType,
                (mask & 2) != 0 ? value.username : username);
    }
    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof NowPlayingInfoDTO)) return false;
        NowPlayingInfoDTO value = (NowPlayingInfoDTO) other;
        return serviceType == value.serviceType && Objects.equals(username, value.username);
    }
    @Override public int hashCode() { return 31 * serviceType.hashCode() + (username == null ? 0 : username.hashCode()); }
    @Override public String toString() { return "NowPlayingInfoDTO(serviceType=" + serviceType + ", username=" + username + ")"; }
}
