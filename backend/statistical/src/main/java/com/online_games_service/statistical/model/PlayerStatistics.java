package com.online_games_service.statistical.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Represents a player's game statistics for a specific game type.
 * Each document tracks the total games played and won for a player.
 */
@Document(collection = "player_statistics")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerStatistics {

    @Id
    private String id;

    /**
     * The player's account ID (not the username, but the actual user ID).
     * Indexed for fast lookups.
     */
    @Indexed
    private String playerId;

    /**
     * The player's username for display purposes.
     */
    private String username;

    /**
     * The type of game (e.g., "MAKAO", "LUDO").
     */
    @Indexed
    private String gameType;

    /**
     * Total number of games played by this player.
     */
    @Indexed
    private int gamesPlayed;

    /**
     * Total number of games won by this player (1st place).
     */
    @Indexed
    private int gamesWon;

    public PlayerStatistics(String playerId, String username, String gameType) {
        this.playerId = playerId;
        this.username = username;
        this.gameType = gameType;
        this.gamesPlayed = 0;
        this.gamesWon = 0;
    }

    /**
     * Increment games played count.
     */
    public void incrementGamesPlayed() {
        this.gamesPlayed++;
    }

    /**
     * Increment games won count.
     */
    public void incrementGamesWon() {
        this.gamesWon++;
    }

    /**
     * Calculate win ratio as a percentage.
     * @return Win ratio percentage (0-100), or 0 if no games played.
     */
    public double getWinRatio() {
        if (gamesPlayed == 0) {
            return 0.0;
        }
        return (double) gamesWon / gamesPlayed * 100.0;
    }
}
