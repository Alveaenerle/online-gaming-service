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
        Assert.assertEquals(game.getRoomId(), roomId);
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
    @Test
    public void shouldReturnUnmodifiablePlayersHands() {
        // Given
        MakaoGame game = new MakaoGame("room_1", Map.of("p1", "P1"), "p1", 4);

        // When
        Map<String, List<Card>> hands = game.getPlayersHands();

        // Then
        Assert.assertThrows(UnsupportedOperationException.class, () -> hands.put("p2", Collections.emptyList()));
    }

    // Tests depending on direct pile access removed; current model exposes only hands and current card
}