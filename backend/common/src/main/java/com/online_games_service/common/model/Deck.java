package com.online_games_service.common.model;

import com.online_games_service.common.enums.CardRank;
import com.online_games_service.common.enums.CardSuit;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck implements Serializable {

    private static final long serialVersionUID = 1L;

    private final List<Card> cards = new ArrayList<>();

    private final int baseDeckCount;

    public Deck() {
        this(1);
    }

    public Deck(int deckCount) {
        this.baseDeckCount = Math.max(1, deckCount);
        reset();
        shuffle();
    }

    public Deck(List<Card> initialCards) {
        this.baseDeckCount = 0;
        if (initialCards != null) {
            addCards(initialCards);
        }
    }

    protected void addStandard52() {
        for (CardSuit suit : CardSuit.values()) {
            for (CardRank rank : CardRank.values()) {
                cards.add(new Card(suit, rank));
            }
        }
    }

    public void shuffle() {
        Collections.shuffle(cards);
    }

    public Card draw() {
        if (cards.isEmpty()) {
            return null;
        }
        return cards.remove(cards.size() - 1);
    }

    public void addCard(Card card) {
        if (card != null) {
            cards.add(card);
        }
    }

    public void addCards(List<Card> newCards) {
        if (newCards == null || newCards.isEmpty()) {
            return;
        }
        for (Card card : newCards) {
            if (card != null) {
                cards.add(card);
            }
        }
    }

    public void clear() {
        cards.clear();
    }

    public boolean removeCard(Card card) {
        if (card == null) {
            return false;
        }
        return cards.remove(card);
    }

    public void reset() {
        cards.clear();
        for (int i = 0; i < baseDeckCount; i++) {
            addStandard52();
        }
    }

    public int size() {
        return cards.size();
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }

    public List<Card> getCards() {
        return Collections.unmodifiableList(cards);
    }
}
