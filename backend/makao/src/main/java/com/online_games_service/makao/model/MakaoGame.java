package com.online_games_service.makao.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

@Data
@NoArgsConstructor
public class MakaoGame implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private RoomStatus status; 
    
    private List<String> playerIds = new ArrayList<>();
    private String currentPlayerId;

    private Map<String, List<Card>> playersHands = new HashMap<>();
    private List<Card> drawPile = new ArrayList<>();
    private List<Card> discardPile = new ArrayList<>();

    private int pendingDrawCount = 0;
    private int pendingSkipTurns = 0;
    
    private CardRank demandedRank = null; 
    private CardSuit demandedSuit = null; 
    
    private String winnerId;

    public MakaoGame(String id, List<String> playerIds) {
        this.id = id;
        this.playerIds = playerIds;
        
        this.status = RoomStatus.PLAYING; 

        if (!playerIds.isEmpty()) {
            this.currentPlayerId = playerIds.get(0);
        }

        for (String playerId : playerIds) {
            playersHands.put(playerId, new ArrayList<>());
        }
    }

    @JsonIgnore
    public Card getTopCard() {
        if (discardPile == null || discardPile.isEmpty()) {
            return null;
        }
        return discardPile.get(discardPile.size() - 1);
    }

    public List<Card> getDiscardPile() {
        return Collections.unmodifiableList(discardPile);
    }

    public void addToDiscardPile(Card card) {
        this.discardPile.add(card);
    }

    public List<Card> getDrawPile() {
        return Collections.unmodifiableList(drawPile);
    }

    public void addToDrawPile(Card card) {
        this.drawPile.add(card);
    }

    public Map<String, List<Card>> getPlayersHands() {
        return Collections.unmodifiableMap(playersHands);
    }

    public void addCardToHand(String playerId, Card card) {
        if (playersHands.containsKey(playerId)) {
            playersHands.get(playerId).add(card);
        }
    }
}