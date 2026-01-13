package com.online_games_service.social.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for responding to a game invite.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RespondGameInviteRequest {
    
    @NotBlank(message = "Invite ID is required")
    private String inviteId;
}
