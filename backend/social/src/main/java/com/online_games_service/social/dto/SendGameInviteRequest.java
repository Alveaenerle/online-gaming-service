package com.online_games_service.social.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for sending a game invite.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendGameInviteRequest {
    
    @NotBlank(message = "Target user ID is required")
    private String targetUserId;
    
    @NotBlank(message = "Lobby ID is required")
    private String lobbyId;
    
    private String lobbyName;
    
    private String gameType;
}
