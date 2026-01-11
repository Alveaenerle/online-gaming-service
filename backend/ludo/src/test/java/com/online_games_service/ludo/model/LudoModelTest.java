package com.online_games_service.ludo.model;

import com.online_games_service.common.enums.RoomStatus;
import com.online_games_service.ludo.dto.LudoGameStateMessage;
import com.online_games_service.ludo.enums.PlayerColor;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class LudoModelTest {

    @Test
    public void shouldCreateLudoGameResult() {
        // Given
        String gameId = "g1";
        int maxPlayers = 4;
        Map<String, String> players = Map.of("p1", "Player1");
        String winnerId = "p1";
        Map<String, Integer> placement = Map.of("p1", 1);

        // When
        LudoGameResult result = new LudoGameResult(gameId, maxPlayers, players, winnerId, placement);
        
        // Then
        Assert.assertEquals(result.getGameId(), gameId);
        Assert.assertEquals(result.getMaxPlayers(), maxPlayers);
        Assert.assertEquals(result.getWinnerId(), winnerId);
        Assert.assertEquals(result.getPlayers(), players);
        Assert.assertEquals(result.getPlacement(), placement);
    }

    @Test
    public void shouldCreateLudoGameStateMessage() {
        // Given
        LudoGameStateMessage msg = new LudoGameStateMessage();
        msg.setGameId("g1");
        msg.setStatus(RoomStatus.PLAYING);
        msg.setCurrentPlayerColor(PlayerColor.RED);
        msg.setCurrentPlayerId("p1");
        msg.setLastDiceRoll(6);
        msg.setDiceRolled(true);
        msg.setWaitingForMove(true);
        msg.setRollsLeft(0);
        msg.setPlayers(new ArrayList<>());
        msg.setUsernames(new HashMap<>());
        msg.setWinnerId(null);
        msg.setCapturedUserId("p2"); 

        // Then 
        Assert.assertEquals(msg.getGameId(), "g1");
        Assert.assertEquals(msg.getStatus(), RoomStatus.PLAYING);
        Assert.assertEquals(msg.getCurrentPlayerColor(), PlayerColor.RED);
        Assert.assertEquals(msg.getCurrentPlayerId(), "p1");
        Assert.assertEquals(msg.getLastDiceRoll(), 6);
        Assert.assertTrue(msg.isDiceRolled());
        Assert.assertTrue(msg.isWaitingForMove());
        Assert.assertEquals(msg.getRollsLeft(), 0);
        Assert.assertNotNull(msg.getPlayers());
        Assert.assertNotNull(msg.getUsernames());
        Assert.assertNull(msg.getWinnerId());
        Assert.assertEquals(msg.getCapturedUserId(), "p2");
    }

    @Test
    public void testLudoPawnModel() {
        // Given
        LudoPawn pawn = new LudoPawn(1, 13, PlayerColor.BLUE, 0, false, false);
        
        // When & Then 
        pawn.setPosition(18);
        pawn.setStepsMoved(5);
        pawn.setInHome(true);
        pawn.setInBase(false);

        Assert.assertEquals(pawn.getId(), 1);
        Assert.assertEquals(pawn.getPosition(), 18);
        Assert.assertEquals(pawn.getColor(), PlayerColor.BLUE);
        Assert.assertTrue(pawn.isInHome());
        Assert.assertFalse(pawn.isInBase());
        
        LudoPawn pawn2 = new LudoPawn(1, 18, PlayerColor.BLUE, 5, false, true);
        Assert.assertEquals(pawn, pawn2);
        Assert.assertEquals(pawn.hashCode(), pawn2.hashCode());
        Assert.assertNotNull(pawn.toString());
    }

    @Test
    public void testLudoPlayerModel() {
        // Given
        LudoPlayer player = new LudoPlayer("u1", PlayerColor.GREEN);
        
        // When
        player.setBot(true);
        
        // Then
        Assert.assertEquals(player.getUserId(), "u1");
        Assert.assertEquals(player.getColor(), PlayerColor.GREEN);
        Assert.assertTrue(player.isBot());
        Assert.assertEquals(player.getPawns().size(), 4);
        Assert.assertFalse(player.hasAllPawnsInHome());
        
        LudoPlayer player2 = new LudoPlayer("u1", PlayerColor.GREEN);
        player2.setBot(true);
        Assert.assertNotNull(player.toString());
    }

    @Test
    public void testLudoGameResultModel() {
        // Given
        LudoGameResult result = new LudoGameResult();
        result.setGameId("g1");
        result.setMaxPlayers(4);
        result.setWinnerId("w1");
        result.setPlayers(Map.of("p1", "P1"));
        result.setPlacement(Map.of("p1", 1));

        // Then
        Assert.assertEquals(result.getGameId(), "g1");
        Assert.assertEquals(result.getMaxPlayers(), 4);
        Assert.assertNotNull(result.toString());
    }

    @Test
    public void testLudoGameStateMessageDTO() {
        // Given
        LudoGameStateMessage msg = new LudoGameStateMessage(
                "r1", RoomStatus.PLAYING, PlayerColor.RED, "p1", 
                6, true, true, 0, new ArrayList<>(), new HashMap<>(), null, null
        );

        // When
        msg.setRollsLeft(3);

        // Then
        Assert.assertEquals(msg.getGameId(), "r1");
        Assert.assertEquals(msg.getRollsLeft(), 3);
        Assert.assertNotNull(msg.toString());
        
        LudoGameStateMessage msg2 = new LudoGameStateMessage();
        msg2.setGameId("r1");
        Assert.assertNotEquals(msg, new Object()); 
    }
}