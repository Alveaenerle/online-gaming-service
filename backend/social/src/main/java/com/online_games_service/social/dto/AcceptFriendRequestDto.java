package com.online_games_service.social.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AcceptFriendRequestDto {
    
    @NotBlank(message = "Request ID is required")
    private String requestId;
}
