package com.online_games_service.ludo.dto;

import com.online_games_service.common.enums.RoomStatus;
import com.online_games_service.ludo.enums.PlayerColor;
import com.online_games_service.ludo.model.LudoPlayer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LudoGameStateMessage {
    private String gameId;
    private RoomStatus status;
    private PlayerColor currentPlayerColor;
    private String currentPlayerId;

    private int lastDiceRoll;
    private boolean diceRolled;
    private boolean waitingForMove;
    private int rollsLeft;

    private List<LudoPlayer> players;
    private Map<String, String> usernames;
    private String winnerId;

    private String capturedUserId;

    // Turn timer - remaining seconds for current player's turn (null for bots)
    private Integer turnRemainingSeconds;
    // Turn start time in milliseconds (for accurate client-side timer calculation)
    private Long turnStartTime;
    // Player avatars - playerId -> avatarId (e.g., "avatar_1.png" or "bot_avatar.svg")
    private Map<String, String> playersAvatars;
}