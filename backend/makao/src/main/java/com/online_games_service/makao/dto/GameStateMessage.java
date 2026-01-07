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
}
