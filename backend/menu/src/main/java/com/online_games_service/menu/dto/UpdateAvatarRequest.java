package com.online_games_service.menu.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateAvatarRequest {
    @NotBlank(message = "Avatar ID is required")
    private String avatarId;
}