package com.online_games_service.menu.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.online_games_service.common.enums.GameType;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JoinGameRequest {
    private GameType gameType;
    private int maxPlayers;

    @NotNull(message = "isRandom field is required")
    @JsonProperty("isRandom")
    private boolean isRandom;

    private String accessCode;
}