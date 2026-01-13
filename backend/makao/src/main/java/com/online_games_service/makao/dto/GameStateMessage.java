package com.online_games_service.makao.dto;

import com.online_games_service.common.enums.CardRank;
import com.online_games_service.common.enums.CardSuit;
import com.online_games_service.common.enums.RoomStatus;
import com.online_games_service.common.model.Card;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameStateMessage {
    private String roomId;
    private String activePlayerId;
    private Card currentCard;
    private List<PlayerCardView> myCards;
    private Map<String, Integer> playersCardsAmount;
    private Map<String, Integer> playersSkipTurns;
    private boolean specialEffectActive;
    private CardRank demandedRank;
    private CardSuit demandedSuit;
    private Map<String, Integer> ranking;
    private Map<String, Integer> placement;
    private List<String> losers;
    private RoomStatus status;
    private int drawDeckCardsAmount;
    private int discardDeckCardsAmount;
    // Move history log - describes the last action taken (e.g., "Player1 played Ace of Hearts")
    private String lastMoveLog;
    // Special effect notification - explicitly describes active effects (e.g., "Player 1 skips a turn due to a 4")
    private String effectNotification;
    // List of recent move logs for move history display
    private List<String> moveHistory;
    // Turn timer - seconds remaining for current player's turn (null for bots)
    private Integer turnRemainingSeconds;
    // Turn start timestamp - when the current turn started (for accurate client-side calculation)
    private Long turnStartTime;
    // MAKAO status - player who has only 1 card left (MAKAO)
    private String makaoPlayerId;
    // Bot thinking state - ID of bot currently "thinking" (null if no bot thinking)
    private String botThinkingPlayerId;
    // Player turn order - list of player IDs in their turn order (human always first)
    private List<String> playerOrder;
    // Player usernames - playerId -> username
    private Map<String, String> playersUsernames;
    // Player avatars - playerId -> avatarId (e.g., "avatar_1.png" or "bot_avatar.png")
    private Map<String, String> playersAvatars;
}
