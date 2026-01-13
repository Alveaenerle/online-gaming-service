package com.online_games_service.social.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for game invite responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameInviteDto {
    private String id;
    private String senderId;
    private String senderUsername;
    private String targetId;
    private String lobbyId;
    private String lobbyName;
    private String gameType;
    private String accessCode;
    private long createdAt;
}
