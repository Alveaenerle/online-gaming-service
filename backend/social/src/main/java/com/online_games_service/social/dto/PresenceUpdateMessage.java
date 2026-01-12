package com.online_games_service.social.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PresenceUpdateMessage {
    private String userId;
    private PresenceStatus status;

    public enum PresenceStatus {
        ONLINE,
        OFFLINE
    }
}
