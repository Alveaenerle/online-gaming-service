package com.online_games_service.makao.model;

import com.online_games_service.common.model.Deck;

public class MakaoDeck extends Deck {

    public MakaoDeck(int playerCount) {
        super(playerCount > 4 ? 2 : 1);
    }

    public MakaoDeck() {
        super(1);
    }

    public MakaoDeck(java.util.List<com.online_games_service.common.model.Card> cards) {
        super(cards);
    }
}
