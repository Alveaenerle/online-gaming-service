package com.online_games_service.makao.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Message sent to a player when they are kicked from the game due to turn timeout.
 * This allows the frontend to show a proper notification modal.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerTimeoutMessage {
    /** The room ID the player was kicked from */
    private String roomId;
    
    /** The player ID who was kicked */
    private String playerId;
    
    /** The bot ID that replaced the player */
    private String replacedByBotId;
    
    /** Human-readable message */
    private String message;
    
    /** Indicates this is a timeout notification */
    private final String type = "PLAYER_TIMEOUT";
}
