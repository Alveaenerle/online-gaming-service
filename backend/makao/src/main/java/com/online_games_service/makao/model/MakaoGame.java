package com.online_games_service.makao.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.online_games_service.common.enums.CardRank;
import com.online_games_service.common.enums.CardSuit;
import com.online_games_service.common.enums.RoomStatus;
import com.online_games_service.common.model.Card;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MakaoGame implements Serializable {

    private static final long serialVersionUID = 1L;

    private String roomId;
    private String gameId;
    private RoomStatus status;
    private String hostUserId;
    private int maxPlayers;

    private List<String> playersOrderIds = new ArrayList<>();
    private String activePlayerId;
    private boolean reverseMovement = false;
    private boolean specialEffectActive = false;
    private List<Card> activePlayerPlayableCards = new ArrayList<>();
    private Card drawnCard = null;

    private Map<String, Integer> playersSkipTurns = new HashMap<>();
    private Map<String, String> playersUsernames = new HashMap<>(); // playerId -> username
    private Map<String, List<Card>> playersHands = new HashMap<>();

    private MakaoDeck drawDeck;
    private MakaoDeck discardDeck;

    private int pendingDrawCount = 0;
    private int pendingSkipTurns = 0;

    private CardRank demandedRank = null;
    private CardSuit demandedSuit = null;

    // playerId -> score (winner has 0, others get summed card values)
    private Map<String, Integer> ranking = new HashMap<>();
    // playerId -> placement (1 = best score)
    private Map<String, Integer> placement = new HashMap<>();
    private List<String> losers = new ArrayList<>();

    private int botCounter = 0;

    // Move history and effect tracking for notifications
    private String lastMoveLog;
    private String effectNotification;
    private List<String> moveHistory = new ArrayList<>();
    private static final int MAX_MOVE_HISTORY = 20;

    public void addMoveLog(String moveLog) {
        this.lastMoveLog = moveLog;
        if (moveLog != null && !moveLog.isBlank()) {
            this.moveHistory.add(moveLog);
            while (this.moveHistory.size() > MAX_MOVE_HISTORY) {
                this.moveHistory.remove(0);
            }
        }
    }

    public void setEffectNotification(String notification) {
        this.effectNotification = notification;
    }

    public void clearNotifications() {
        this.lastMoveLog = null;
        this.effectNotification = null;
    }

    public MakaoGame(String roomId, Map<String, String> players, String hostUserId, int maxPlayers) {
        this.roomId = roomId;
        this.gameId = "MAKAO-" + UUID.randomUUID();
        if (maxPlayers < 2) {
            throw new IllegalArgumentException("maxPlayers must be at least 2");
        }
        this.maxPlayers = maxPlayers;
        this.hostUserId = hostUserId;

        if (players != null && !players.isEmpty()) {
            this.playersUsernames.putAll(players);
        }

        int missing = Math.max(0, maxPlayers - this.playersUsernames.size());
        while (missing > 0) {
            botCounter++;
            String botId = "bot-" + botCounter;
            if (!this.playersUsernames.containsKey(botId)) {
                this.playersUsernames.put(botId, "Bot " + botCounter);
                missing--;
            }
        }

        if (!this.playersUsernames.isEmpty()) {
            List<String> randomized = new ArrayList<>(this.playersUsernames.keySet());
            Collections.shuffle(randomized);
            this.playersOrderIds = randomized;
        }

        this.status = RoomStatus.PLAYING;

        if (!this.playersOrderIds.isEmpty()) {
            this.activePlayerId = this.playersOrderIds.get(0);
        }

        for (String playerId : this.playersOrderIds) {
            playersHands.put(playerId, new ArrayList<>());
            playersSkipTurns.put(playerId, 0);
        }

        this.drawDeck = new MakaoDeck(this.playersOrderIds.size());
        this.discardDeck = new MakaoDeck(new ArrayList<>());

        dealInitialCards(5);
        moveTopCardToDiscard();
    }

    private void dealInitialCards(int cardsPerPlayer) {
        if (cardsPerPlayer <= 0 || drawDeck == null) {
            return;
        }
        for (int i = 0; i < cardsPerPlayer; i++) {
            for (String playerId : playersOrderIds) {
                Card drawn = drawDeck.draw();
                if (drawn != null) {
                    playersHands.get(playerId).add(drawn);
                }
            }
        }
    }

    private void moveTopCardToDiscard() {
        if (drawDeck == null) {
            return;
        }
        if (discardDeck == null) {
            discardDeck = new MakaoDeck(new ArrayList<>());
        }

        Card drawn;
        do {
            drawn = drawDeck.draw();
            if (drawn == null) {
                return;
            }
            discardDeck.addCard(drawn);
        } while (isSpecialCard(drawn));
    }

    public Map<String, List<Card>> getPlayersHands() {
        return Collections.unmodifiableMap(playersHands);
    }

    public void addCardToHand(String playerId, Card card) {
        if (playersHands.containsKey(playerId)) {
            playersHands.get(playerId).add(card);
        }
    }

    public Card getCurrentCard() {
        if (discardDeck == null || discardDeck.isEmpty()) {
            return null;
        }
        List<Card> discardCards = discardDeck.getCards();
        return discardCards.get(discardCards.size() - 1);
    }

    private boolean isSpecialCard(Card card) {
        if (card == null || card.getRank() == null || card.getSuit() == null) {
            return false;
        }
        switch (card.getRank()) {
            case TWO:
            case THREE:
            case FOUR:
            case JACK:
            case ACE:
                return true;
            case KING:
                return card.getSuit() == CardSuit.HEARTS || card.getSuit() == CardSuit.SPADES;
            default:
                return false;
        }
    }
}