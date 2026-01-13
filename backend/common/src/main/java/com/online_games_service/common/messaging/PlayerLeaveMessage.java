package com.online_games_service.common.messaging;

/**
 * Message published when a player leaves or times out of a game.
 * This is used to notify the Menu service to update the GameRoom accordingly.
 *
 * @param roomId   The room ID
 * @param playerId The player ID who left/timed out
 * @param reason   The reason for leaving (VOLUNTARY, TIMEOUT, DISCONNECT)
 */
public record PlayerLeaveMessage(
        String roomId,
        String playerId,
        LeaveReason reason
) {
    public enum LeaveReason {
        VOLUNTARY,
        TIMEOUT,
        DISCONNECT
    }
}
