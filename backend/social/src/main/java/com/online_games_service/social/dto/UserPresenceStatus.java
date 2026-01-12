package com.online_games_service.social.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPresenceStatus {
    private String userId;
    private boolean online;
}
