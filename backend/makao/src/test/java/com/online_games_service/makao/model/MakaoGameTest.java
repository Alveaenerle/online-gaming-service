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
import java.util.Map;

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
        Card card = new Card(CardSuit.HEARTS, CardRank.ACE);
        game.addToDiscardPile(card);

        // When
        Card topCard = game.getTopCard();

        // Then
        Assert.assertEquals(topCard, card);
    }

    @Test
    public void shouldReturnNullTopCardWhenDiscardPileIsEmpty() {
        // Given
        MakaoGame game = new MakaoGame("room_1", List.of("p1"));

        // When
        Card topCard = game.getTopCard();

        // Then
        Assert.assertNull(topCard);
    }

    @Test
    public void shouldHandleEmptyDiscardPileGracefully() {
        // Given
        MakaoGame game = new MakaoGame("room_1", List.of("p1"));
        // Ensure discard pile is empty (it is by default)

        // When
        Card topCard = game.getTopCard();

        // Then
        Assert.assertNull(topCard);
    }

    @Test
    public void shouldAddCardToDrawPile() {
        // Given
        MakaoGame game = new MakaoGame("room_1", List.of("p1"));
        Card card = new Card(CardSuit.SPADES, CardRank.KING);

        // When
        game.addToDrawPile(card);

        // Then
        Assert.assertEquals(game.getDrawPile().size(), 1);
        Assert.assertEquals(game.getDrawPile().get(0), card);
    }

    @Test
    public void shouldAddCardToPlayerHand() {
        // Given
        String playerId = "p1";
        MakaoGame game = new MakaoGame("room_1", List.of(playerId));
        Card card = new Card(CardSuit.CLUBS, CardRank.QUEEN);

        // When
        game.addCardToHand(playerId, card);

        // Then
        Assert.assertEquals(game.getPlayersHands().get(playerId).size(), 1);
        Assert.assertEquals(game.getPlayersHands().get(playerId).get(0), card);
    }

    @Test
    public void shouldNotAddCardToUnknownPlayerHand() {
        // Given
        MakaoGame game = new MakaoGame("room_1", List.of("p1"));
        Card card = new Card(CardSuit.DIAMONDS, CardRank.JACK);

        // When
        game.addCardToHand("unknown_player", card);

        // Then
        Assert.assertFalse(game.getPlayersHands().containsKey("unknown_player"));
    }

    @Test
    public void shouldSetAndGetGameProperties() {
        // Given
        MakaoGame game = new MakaoGame();
        game.setId("test_id");
        game.setStatus(RoomStatus.FINISHED);
        game.setCurrentPlayerId("p2");
        game.setPendingDrawCount(2);
        game.setPendingSkipTurns(1);
        game.setDemandedRank(CardRank.SEVEN);
        game.setDemandedSuit(CardSuit.HEARTS);
        game.setWinnerId("p1");

        // Then
        Assert.assertEquals(game.getId(), "test_id");
        Assert.assertEquals(game.getStatus(), RoomStatus.FINISHED);
        Assert.assertEquals(game.getCurrentPlayerId(), "p2");
        Assert.assertEquals(game.getPendingDrawCount(), 2);
        Assert.assertEquals(game.getPendingSkipTurns(), 1);
        Assert.assertEquals(game.getDemandedRank(), CardRank.SEVEN);
        Assert.assertEquals(game.getDemandedSuit(), CardSuit.HEARTS);
        Assert.assertEquals(game.getWinnerId(), "p1");
    }

    @Test
    public void shouldReturnUnmodifiableDrawPile() {
        // Given
        MakaoGame game = new MakaoGame("room_1", List.of("p1"));

        // When
        List<Card> drawPile = game.getDrawPile();

        // Then
        Assert.assertThrows(UnsupportedOperationException.class, () -> drawPile.add(new Card(CardSuit.HEARTS, CardRank.ACE)));
    }

    @Test
    public void shouldReturnUnmodifiableDiscardPile() {
        // Given
        MakaoGame game = new MakaoGame("room_1", List.of("p1"));

        // When
        List<Card> discardPile = game.getDiscardPile();

        // Then
        Assert.assertThrows(UnsupportedOperationException.class, () -> discardPile.add(new Card(CardSuit.HEARTS, CardRank.ACE)));
    }

    @Test
    public void shouldReturnUnmodifiablePlayersHands() {
        // Given
        MakaoGame game = new MakaoGame("room_1", List.of("p1"));

        // When
        Map<String, List<Card>> hands = game.getPlayersHands();

        // Then
        Assert.assertThrows(UnsupportedOperationException.class, () -> hands.put("p2", Collections.emptyList()));
    }

    @Test
    public void shouldInitializeWithDefaultValues() {
        // Given
        MakaoGame game = new MakaoGame();

        // Then
        Assert.assertNotNull(game.getPlayerIds());
        Assert.assertTrue(game.getPlayerIds().isEmpty());
        Assert.assertNotNull(game.getPlayersHands());
        Assert.assertTrue(game.getPlayersHands().isEmpty());
        Assert.assertNotNull(game.getDrawPile());
        Assert.assertTrue(game.getDrawPile().isEmpty());
        Assert.assertNotNull(game.getDiscardPile());
        Assert.assertTrue(game.getDiscardPile().isEmpty());
        Assert.assertEquals(game.getPendingDrawCount(), 0);
        Assert.assertEquals(game.getPendingSkipTurns(), 0);
        Assert.assertNull(game.getDemandedRank());
        Assert.assertNull(game.getDemandedSuit());
    }
}