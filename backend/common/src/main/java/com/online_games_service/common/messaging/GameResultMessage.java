package com.online_games_service.common.messaging;

import java.util.Map;

/**
 * Message sent when a game ends to record statistics.
 * Contains all the information needed to update player statistics.
 */
public record GameResultMessage(
        String roomId,
        String gameType,
        Map<String, String> participants, // playerId -> username
        Map<String, Integer> placements,  // playerId -> placement (1st, 2nd, etc.)
        String winnerId                    // ID of the player who won (1st place)
) {
}
