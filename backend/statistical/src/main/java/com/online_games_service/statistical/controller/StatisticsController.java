package com.online_games_service.statistical.controller;

import com.online_games_service.statistical.dto.PlayerAllStatisticsDto;
import com.online_games_service.statistical.dto.PlayerStatisticsDto;
import com.online_games_service.statistical.dto.RankingsDto;
import com.online_games_service.statistical.service.StatisticsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class StatisticsController {

    private static final String USER_ID_ATTRIBUTE = "userId";
    private static final int DEFAULT_RANKING_LIMIT = 30;
    private static final int MAX_RANKING_LIMIT = 100;

    private final StatisticsService statisticsService;

    /**
     * Get statistics for the current authenticated user for a specific game type.
     */
    @GetMapping("/me/{gameType}")
    public ResponseEntity<PlayerStatisticsDto> getMyStatistics(
            HttpServletRequest request,
            @PathVariable String gameType) {
        
        String userId = (String) request.getAttribute(USER_ID_ATTRIBUTE);
        if (userId == null) {
            log.warn("No userId found in request for /me endpoint");
            return ResponseEntity.status(401).build();
        }

        PlayerStatisticsDto stats = statisticsService.getPlayerStatistics(userId, gameType.toUpperCase());
        return ResponseEntity.ok(stats);
    }

    /**
     * Get all statistics for the current authenticated user.
     */
    @GetMapping("/me")
    public ResponseEntity<PlayerAllStatisticsDto> getAllMyStatistics(HttpServletRequest request) {
        String userId = (String) request.getAttribute(USER_ID_ATTRIBUTE);
        if (userId == null) {
            log.warn("No userId found in request for /me endpoint");
            return ResponseEntity.status(401).build();
        }

        PlayerAllStatisticsDto stats = statisticsService.getAllPlayerStatistics(userId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Get statistics for a specific player and game type.
     * This endpoint is public and can be used to view other players' stats.
     */
    @GetMapping("/player/{playerId}/{gameType}")
    public ResponseEntity<PlayerStatisticsDto> getPlayerStatistics(
            @PathVariable String playerId,
            @PathVariable String gameType) {
        
        PlayerStatisticsDto stats = statisticsService.getPlayerStatistics(playerId, gameType.toUpperCase());
        return ResponseEntity.ok(stats);
    }

    /**
     * Get all statistics for a specific player.
     */
    @GetMapping("/player/{playerId}")
    public ResponseEntity<PlayerAllStatisticsDto> getAllPlayerStatistics(@PathVariable String playerId) {
        PlayerAllStatisticsDto stats = statisticsService.getAllPlayerStatistics(playerId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Get rankings for a specific game type.
     */
    @GetMapping("/rankings/{gameType}")
    public ResponseEntity<RankingsDto> getRankings(
            @PathVariable String gameType,
            @RequestParam(defaultValue = "30") int limit) {
        
        int effectiveLimit = Math.min(Math.max(limit, 1), MAX_RANKING_LIMIT);
        RankingsDto rankings = statisticsService.getRankings(gameType.toUpperCase(), effectiveLimit);
        return ResponseEntity.ok(rankings);
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
