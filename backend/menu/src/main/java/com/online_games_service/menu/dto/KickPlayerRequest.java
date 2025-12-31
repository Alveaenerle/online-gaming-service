package com.online_games_service.menu.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class KickPlayerRequest {
    @NotBlank(message = "User ID to kick is required")
    private String userId;
}