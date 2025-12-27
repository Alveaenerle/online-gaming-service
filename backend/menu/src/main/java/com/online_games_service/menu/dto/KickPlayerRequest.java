package com.online_games_service.menu.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class KickPlayerRequest {
    @NotBlank(message = "Username to kick is required")
    private String username;
}