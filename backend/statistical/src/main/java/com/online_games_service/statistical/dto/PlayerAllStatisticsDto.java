package com.online_games_service.statistical.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for returning all statistics for a player across all game types.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerAllStatisticsDto {
    private String playerId;
    private List<PlayerStatisticsDto> statistics;
}
