package com.online_games_service.makao.model;

import com.online_games_service.common.enums.CardRank;
import com.online_games_service.common.enums.CardSuit;
import com.online_games_service.common.enums.RoomStatus;
import com.online_games_service.common.model.Card;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class MakaoGameTest {

    @Test
    void shouldInitializeGameCorrectly() {
        // Given
        String roomId = "room_1";
        List<String> players = Arrays.asList("player1", "player2");

        // When
        MakaoGame game = new MakaoGame(roomId, players);

        // Then
        assertEquals(roomId, game.getId());
        assertEquals(RoomStatus.PLAYING, game.getStatus());
        assertEquals("player1", game.getCurrentPlayerId()); // Pierwszy z listy zaczyna
        assertEquals(2, game.getPlayersHands().size()); // Ręce zostały zainicjowane
        assertTrue(game.getPlayersHands().get("player1").isEmpty()); // Ale są puste
    }

    @Test
    void shouldHandleEmptyPlayerList() {
        // Given
        String roomId = "room_empty";
        List<String> players = Collections.emptyList();

        // When
        MakaoGame game = new MakaoGame(roomId, players);

        // Then
        assertNull(game.getCurrentPlayerId());
        assertTrue(game.getPlayersHands().isEmpty());
    }

    @Test
    void shouldReturnTopCardFromDiscardPile() {
        // Given
        MakaoGame game = new MakaoGame("room_1", List.of("p1"));
        Card card1 = new Card(CardSuit.HEARTS, CardRank.TWO);
        Card card2 = new Card(CardSuit.SPADES, CardRank.ACE);
        
        game.getDiscardPile().add(card1);
        game.getDiscardPile().add(card2);

        // When
        Card topCard = game.getTopCard();

        // Then
        assertThat(topCard).isEqualTo(card2);
        assertEquals(CardRank.ACE, topCard.getRank());
    }

    @Test
    void shouldReturnNullTopCardWhenPileIsEmpty() {
        // Given
        MakaoGame game = new MakaoGame("room_1", List.of("p1"));

        // When
        Card topCard = game.getTopCard();

        // Then
        assertNull(topCard);
    }
}