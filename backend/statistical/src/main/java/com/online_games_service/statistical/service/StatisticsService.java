package com.online_games_service.statistical.service;

import com.online_games_service.statistical.dto.PlayerAllStatisticsDto;
import com.online_games_service.statistical.dto.PlayerStatisticsDto;
import com.online_games_service.statistical.dto.RankingsDto;
import com.online_games_service.statistical.model.PlayerStatistics;
import com.online_games_service.statistical.repository.PlayerStatisticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticsService {

    private static final int DEFAULT_RANKING_SIZE = 30;

    private final PlayerStatisticsRepository statisticsRepository;

    /**
     * Record a game result for all participants.
     * Only registered users (non-bots, non-guests) should have their stats recorded.
     *
     * @param gameType The type of game (e.g., "MAKAO", "LUDO")
     * @param participants Map of playerId -> username for all participants
     * @param winnerId The ID of the winner (player who got 1st place)
     */
    public void recordGameResult(String gameType, Map<String, String> participants, String winnerId) {
        if (gameType == null || gameType.isBlank()) {
            log.warn("Cannot record game result: gameType is null or blank");
            return;
        }

        if (participants == null || participants.isEmpty()) {
            log.warn("Cannot record game result: no participants provided");
            return;
        }

        log.info("Recording game result for {} with {} participants, winner: {}", 
                gameType, participants.size(), winnerId);

        for (Map.Entry<String, String> entry : participants.entrySet()) {
            String playerId = entry.getKey();
            String username = entry.getValue();

            if (playerId == null || playerId.isBlank()) {
                continue;
            }

            // Skip bots (bot IDs typically contain "bot" or start with "bot_")
            if (isBot(playerId)) {
                log.debug("Skipping bot player: {}", playerId);
                continue;
            }

            PlayerStatistics stats = statisticsRepository
                    .findByPlayerIdAndGameType(playerId, gameType)
                    .orElseGet(() -> new PlayerStatistics(playerId, username, gameType));

            // Update username in case it changed
            if (username != null && !username.isBlank()) {
                stats.setUsername(username);
            }

            stats.incrementGamesPlayed();

            if (playerId.equals(winnerId)) {
                stats.incrementGamesWon();
            }

            statisticsRepository.save(stats);
            log.debug("Updated statistics for player {}: played={}, won={}", 
                    playerId, stats.getGamesPlayed(), stats.getGamesWon());
        }
    }

    /**
     * Check if a player ID represents a bot.
     */
    boolean isBot(String playerId) {
        if (playerId == null) {
            return false;
        }
        String lowerCaseId = playerId.toLowerCase();
        return lowerCaseId.contains("bot") || lowerCaseId.startsWith("bot_");
    }

    /**
     * Get statistics for a specific player and game type.
     */
    public PlayerStatisticsDto getPlayerStatistics(String playerId, String gameType) {
        if (playerId == null || gameType == null) {
            return null;
        }

        return statisticsRepository.findByPlayerIdAndGameType(playerId, gameType)
                .map(this::toDto)
                .orElse(createEmptyStats(playerId, gameType));
    }

    /**
     * Get all statistics for a player across all game types.
     */
    public PlayerAllStatisticsDto getAllPlayerStatistics(String playerId) {
        if (playerId == null) {
            return new PlayerAllStatisticsDto(playerId, List.of());
        }

        List<PlayerStatisticsDto> stats = statisticsRepository.findByPlayerId(playerId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return new PlayerAllStatisticsDto(playerId, stats);
    }

    /**
     * Get rankings for a specific game type.
     */
    public RankingsDto getRankings(String gameType) {
        return getRankings(gameType, DEFAULT_RANKING_SIZE);
    }

    /**
     * Get rankings for a specific game type with custom limit.
     */
    public RankingsDto getRankings(String gameType, int limit) {
        if (gameType == null || gameType.isBlank()) {
            return new RankingsDto(gameType, List.of(), List.of());
        }

        Pageable pageable = PageRequest.of(0, limit);

        List<PlayerStatisticsDto> topByGamesPlayed = statisticsRepository
                .findByGameTypeOrderByGamesPlayedDesc(gameType, pageable)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        List<PlayerStatisticsDto> topByGamesWon = statisticsRepository
                .findByGameTypeOrderByGamesWonDesc(gameType, pageable)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return new RankingsDto(gameType, topByGamesPlayed, topByGamesWon);
    }

    private PlayerStatisticsDto toDto(PlayerStatistics stats) {
        return new PlayerStatisticsDto(
                stats.getPlayerId(),
                stats.getUsername(),
                stats.getGameType(),
                stats.getGamesPlayed(),
                stats.getGamesWon(),
                stats.getWinRatio()
        );
    }

    private PlayerStatisticsDto createEmptyStats(String playerId, String gameType) {
        return new PlayerStatisticsDto(playerId, null, gameType, 0, 0, 0.0);
    }
}
