package com.online_games_service.statistical.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for returning rankings in API responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RankingsDto {
    private String gameType;
    private List<PlayerStatisticsDto> topByGamesPlayed;
    private List<PlayerStatisticsDto> topByGamesWon;
}
