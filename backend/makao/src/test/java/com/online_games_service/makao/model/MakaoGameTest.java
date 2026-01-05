package com.online_games_service.makao.model;

import com.online_games_service.common.enums.CardRank;
import com.online_games_service.common.enums.CardSuit;
import com.online_games_service.common.enums.RoomStatus;
import com.online_games_service.common.model.Card;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MakaoGameTest {

    @Test
    public void shouldInitializeGameCorrectly() {
        // Given
        String roomId = "room_1";
        Map<String, String> players = Map.of("player1", "Player 1", "player2", "Player 2");

        // When
        MakaoGame game = new MakaoGame(roomId, players, "player1", 4);

        // Then
        Assert.assertEquals(game.getId(), roomId);
        Assert.assertEquals(game.getStatus(), RoomStatus.PLAYING);
        Assert.assertTrue(players.containsKey(game.getActivePlayerId()) || game.getActivePlayerId().startsWith("bot-"));
        Assert.assertEquals(game.getPlayersHands().size(), 4);
        Assert.assertEquals(game.getPlayersOrderIds().size(), 4);
        Assert.assertEquals(game.getPlayersHands().get(game.getActivePlayerId()).size(), 5);
        Assert.assertTrue(game.getPlayersHands().containsKey("player1"));
        Assert.assertTrue(game.getPlayersHands().containsKey("player2"));
        Assert.assertTrue(game.getPlayersHands().values().stream().allMatch(hand -> hand.size() == 5));
    }

    @Test
    public void shouldHandleEmptyPlayerList() {
        // Given
        String roomId = "room_empty";
        Map<String, String> players = Collections.emptyMap();

        // When
        MakaoGame game = new MakaoGame(roomId, players, "host", 4);

        // Then
        Assert.assertNotNull(game.getActivePlayerId());
        Assert.assertEquals(game.getPlayersHands().size(), 4);
        Assert.assertEquals(game.getPlayersOrderIds().size(), 4);
        Assert.assertTrue(game.getPlayersHands().values().stream().allMatch(hand -> hand.size() == 5));
    }

    @Test
    public void shouldReturnTopCardFromDiscardPile() {
        // Given
        MakaoGame game = new MakaoGame("room_1", Map.of("p1", "P1"), "p1", 4);
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
        MakaoGame game = new MakaoGame("room_1", Map.of("p1", "P1"), "p1", 4);

        // When
        Card topCard = game.getTopCard();

        // Then
        Assert.assertNull(topCard);
    }

    @Test
    public void shouldHandleEmptyDiscardPileGracefully() {
        // Given
        MakaoGame game = new MakaoGame("room_1", Map.of("p1", "P1"), "p1", 4);
        // Ensure discard pile is empty (it is by default)

        // When
        Card topCard = game.getTopCard();

        // Then
        Assert.assertNull(topCard);
    }

    @Test
    public void shouldAddCardToDrawPile() {
        // Given
        MakaoGame game = new MakaoGame("room_1", Map.of("p1", "P1"), "p1", 4);
        Card card = new Card(CardSuit.SPADES, CardRank.KING);
        int initialSize = game.getDrawPile().size();

        // When
        game.addToDrawPile(card);

        // Then
        Assert.assertEquals(game.getDrawPile().size(), initialSize + 1);
        Assert.assertEquals(game.getDrawPile().get(game.getDrawPile().size() - 1), card);
    }

    @Test
    public void shouldAddCardToPlayerHand() {
        // Given
        String playerId = "p1";
        MakaoGame game = new MakaoGame("room_1", Map.of(playerId, "Player"), playerId, 4);
        Card card = new Card(CardSuit.CLUBS, CardRank.QUEEN);

        // When
        game.addCardToHand(playerId, card);

        // Then
        Assert.assertEquals(game.getPlayersHands().get(playerId).size(), 6);
        Assert.assertEquals(game.getPlayersHands().get(playerId).get(5), card);
    }

    @Test
    public void shouldNotAddCardToUnknownPlayerHand() {
        // Given
        MakaoGame game = new MakaoGame("room_1", Map.of("p1", "P1"), "p1", 4);
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
        game.setActivePlayerId("p2");
        game.setPendingDrawCount(2);
        game.setPendingSkipTurns(1);
        game.setDemandedRank(CardRank.SEVEN);
        game.setDemandedSuit(CardSuit.HEARTS);
        game.setMaxPlayers(6);
        game.getRanking().put("p1", 0);
        game.getLosers().add("p3");

        // Then
        Assert.assertEquals(game.getId(), "test_id");
        Assert.assertEquals(game.getStatus(), RoomStatus.FINISHED);
        Assert.assertEquals(game.getActivePlayerId(), "p2");
        Assert.assertEquals(game.getPendingDrawCount(), 2);
        Assert.assertEquals(game.getPendingSkipTurns(), 1);
        Assert.assertEquals(game.getDemandedRank(), CardRank.SEVEN);
        Assert.assertEquals(game.getDemandedSuit(), CardSuit.HEARTS);
        Assert.assertEquals(game.getMaxPlayers(), 6);
        Assert.assertEquals(game.getRanking().get("p1"), Integer.valueOf(0));
        Assert.assertEquals(game.getLosers(), List.of("p3"));
    }

    @Test
    public void shouldReturnUnmodifiableDrawPile() {
        // Given
        MakaoGame game = new MakaoGame("room_1", Map.of("p1", "P1"), "p1", 4);

        // When
        List<Card> drawPile = game.getDrawPile();

        // Then
        Assert.assertThrows(UnsupportedOperationException.class, () -> drawPile.add(new Card(CardSuit.HEARTS, CardRank.ACE)));
    }

    @Test
    public void shouldReturnUnmodifiableDiscardPile() {
        // Given
        MakaoGame game = new MakaoGame("room_1", Map.of("p1", "P1"), "p1", 4);

        // When
        List<Card> discardPile = game.getDiscardPile();

        // Then
        Assert.assertThrows(UnsupportedOperationException.class, () -> discardPile.add(new Card(CardSuit.HEARTS, CardRank.ACE)));
    }

    @Test
    public void shouldReturnUnmodifiablePlayersHands() {
        // Given
        MakaoGame game = new MakaoGame("room_1", Map.of("p1", "P1"), "p1", 4);

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
        Assert.assertNotNull(game.getPlayersOrderIds());
        Assert.assertTrue(game.getPlayersOrderIds().isEmpty());
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
        Assert.assertTrue(game.getRanking().isEmpty());
        Assert.assertTrue(game.getLosers().isEmpty());
        Assert.assertEquals(game.getMaxPlayers(), 0);
    }
}