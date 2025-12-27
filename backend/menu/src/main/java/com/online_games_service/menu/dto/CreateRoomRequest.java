package com.online_games_service.menu.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.online_games_service.common.enums.GameType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateRoomRequest {
    @NotBlank(message = "Room name is required")
    private String name;

    @NotNull(message = "Game type is required")
    private GameType gameType;

    private int maxPlayers;

    @JsonProperty("isPrivate")
    private boolean isPrivate;
}