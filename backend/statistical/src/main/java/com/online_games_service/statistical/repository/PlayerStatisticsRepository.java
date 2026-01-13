package com.online_games_service.statistical.repository;

import com.online_games_service.statistical.model.PlayerStatistics;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerStatisticsRepository extends MongoRepository<PlayerStatistics, String> {

    /**
     * Find statistics for a specific player and game type.
     */
    Optional<PlayerStatistics> findByPlayerIdAndGameType(String playerId, String gameType);

    /**
     * Find all statistics for a specific player (all game types).
     */
    List<PlayerStatistics> findByPlayerId(String playerId);

    /**
     * Find top players by games played for a specific game type.
     */
    List<PlayerStatistics> findByGameTypeOrderByGamesPlayedDesc(String gameType, Pageable pageable);

    /**
     * Find top players by games won for a specific game type.
     */
    List<PlayerStatistics> findByGameTypeOrderByGamesWonDesc(String gameType, Pageable pageable);

    /**
     * Check if statistics exist for a player and game type.
     */
    boolean existsByPlayerIdAndGameType(String playerId, String gameType);
}
