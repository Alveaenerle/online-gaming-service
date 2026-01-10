package com.online_games_service.social.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendFriendRequestDto {
    
    @NotBlank(message = "Target user ID is required")
    private String targetUserId;
}
