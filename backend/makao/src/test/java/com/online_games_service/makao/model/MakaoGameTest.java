package com.online_games_service.makao.model;

import com.online_games_service.common.enums.CardRank;
import com.online_games_service.common.enums.CardSuit;
import com.online_games_service.common.enums.RoomStatus;
import com.online_games_service.common.model.Card;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MakaoGameTest {

    @Test
    public void shouldInitializeGameCorrectly() {
        // Given
        String roomId = "room_1";
        List<String> players = Arrays.asList("player1", "player2");

        // When
        MakaoGame game = new MakaoGame(roomId, players);

        // Then
        Assert.assertEquals(game.getId(), roomId);
        Assert.assertEquals(game.getStatus(), RoomStatus.PLAYING);
        Assert.assertEquals(game.getCurrentPlayerId(), "player1");
        Assert.assertEquals(game.getPlayersHands().size(), 2);
        Assert.assertTrue(game.getPlayersHands().get("player1").isEmpty());
    }

    @Test
    public void shouldHandleEmptyPlayerList() {
        // Given
        String roomId = "room_empty";
        List<String> players = Collections.emptyList();

        // When
        MakaoGame game = new MakaoGame(roomId, players);

        // Then
        Assert.assertNull(game.getCurrentPlayerId());
        Assert.assertTrue(game.getPlayersHands().isEmpty());
    }

    @Test
    public void shouldReturnTopCardFromDiscardPile() {
        // Given
        MakaoGame game = new MakaoGame("room_1", List.of("p1"));
        Card card1 = new Card(CardSuit.HEARTS, CardRank.TWO);
        Card card2 = new Card(CardSuit.SPADES, CardRank.ACE);
        
        game.addToDiscardPile(card1);
        game.addToDiscardPile(card2);

        // When
        Card topCard = game.getTopCard();

        // Then
        Assert.assertEquals(topCard, card2);
        Assert.assertEquals(topCard.getRank(), CardRank.ACE);
    }

    @Test
    public void shouldReturnNullTopCardWhenPileIsEmpty() {
        // Given
        MakaoGame game = new MakaoGame("room_1", List.of("p1"));

        // When
        Card topCard = game.getTopCard();

        // Then
        Assert.assertNull(topCard);
    }
}