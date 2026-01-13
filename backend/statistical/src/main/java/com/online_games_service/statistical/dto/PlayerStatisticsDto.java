package com.online_games_service.statistical.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for returning player statistics in API responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerStatisticsDto {
    private String playerId;
    private String username;
    private String gameType;
    private int gamesPlayed;
    private int gamesWon;
    private double winRatio;
}
