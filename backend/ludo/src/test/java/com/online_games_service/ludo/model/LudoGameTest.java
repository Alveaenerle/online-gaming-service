package com.online_games_service.ludo.model;

import com.online_games_service.common.enums.RoomStatus;
import com.online_games_service.ludo.dto.LudoGameStateMessage;
import com.online_games_service.ludo.enums.PlayerColor;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LudoGameTest {
    @Test
    public void shouldInitializeGameCorrectly() {
        // Given
        String roomId = "room_1";
        List<String> playerIds = List.of("p1", "p2");
        String hostId = "p1";
        Map<String, String> usernames = Map.of("p1", "Player 1", "p2", "Player 2");

        // When
        LudoGame game = new LudoGame(roomId, playerIds, hostId, usernames);

        // Then
        Assert.assertEquals(game.getRoomId(), roomId);
        Assert.assertEquals(game.getStatus(), RoomStatus.PLAYING);
        Assert.assertEquals(game.getPlayers().size(), 2);
        
        Assert.assertEquals(game.getPlayers().get(0).getColor(), PlayerColor.RED);
        Assert.assertEquals(game.getPlayers().get(1).getColor(), PlayerColor.BLUE);
        
        Assert.assertEquals(game.getCurrentPlayerColor(), PlayerColor.RED);
        Assert.assertEquals(game.getActivePlayerId(), "p1");
    }

    @Test
    public void shouldInitializePawnsForPlayer() {
        // Given
        LudoGame game = new LudoGame("r1", List.of("p1"), "p1", Map.of("p1", "P1"));
        
        // When
        LudoPlayer player = game.getPlayers().get(0);

        // Then
        Assert.assertEquals(player.getPawns().size(), 4);
        Assert.assertTrue(player.getPawns().stream().allMatch(LudoPawn::isInBase));
        Assert.assertEquals(player.getColor(), PlayerColor.RED);
    }

    @Test
    public void shouldGetPlayerById() {
        // Given
        LudoGame game = new LudoGame("r1", List.of("p1", "p2"), "p1", null);
        
        // When
        LudoPlayer found = game.getPlayerById("p2");

        // Then
        Assert.assertNotNull(found);
        Assert.assertEquals(found.getUserId(), "p2");
        Assert.assertEquals(found.getColor(), PlayerColor.BLUE);
    }
    
    @Test
    public void shouldReturnNullForUnknownPlayer() {
        // Given
        LudoGame game = new LudoGame("r1", List.of("p1"), "p1", null);
        
        // When & Then
        Assert.assertNull(game.getPlayerById("unknown"));
    }

    @Test
    public void shouldGetPlayerByColor() {
        // Given
        LudoGame game = new LudoGame("r1", List.of("p1", "p2"), "p1", null);
        
        // When
        LudoPlayer red = game.getPlayerByColor(PlayerColor.RED);
        
        // Then
        Assert.assertNotNull(red);
        Assert.assertEquals(red.getUserId(), "p1");
    }

    @Test
    public void nextColorShouldRotateCorrectly() {
        // Given & When & Then
        Assert.assertEquals(PlayerColor.RED.next(), PlayerColor.BLUE);
        Assert.assertEquals(PlayerColor.BLUE.next(), PlayerColor.GREEN);
        Assert.assertEquals(PlayerColor.GREEN.next(), PlayerColor.YELLOW);
        Assert.assertEquals(PlayerColor.YELLOW.next(), PlayerColor.RED);
    }

    @Test
    public void shouldDetectWinCondition() {
        // Given
        LudoPlayer player = new LudoPlayer("p1", PlayerColor.RED);
        
        // When
        player.getPawns().forEach(p -> p.setInHome(true));
        
        // Then
        Assert.assertTrue(player.hasAllPawnsInHome());
    }

    @Test
    public void shouldNotWinIfOnePawnOutside() {
        // Given
        LudoPlayer player = new LudoPlayer("p1", PlayerColor.RED);
        
        // When
        player.getPawns().forEach(p -> p.setInHome(true));
        player.getPawns().get(0).setInHome(false); 
        
        // Then
        Assert.assertFalse(player.hasAllPawnsInHome());
    }

    @Test
    public void testLudoPawnModelCoverage() {
        // Given
        LudoPawn pawn = new LudoPawn(1, 10, PlayerColor.BLUE, 10, false, false);
        
        // When
        pawn.setPosition(15);
        pawn.setStepsMoved(15);
        pawn.setInHome(true);
        pawn.setInBase(false);

        // Then
        Assert.assertEquals(pawn.getId(), 1);
        Assert.assertEquals(pawn.getPosition(), 15);
        Assert.assertEquals(pawn.getColor(), PlayerColor.BLUE);
        Assert.assertTrue(pawn.isInHome());
        Assert.assertFalse(pawn.isInBase());
        Assert.assertNotNull(pawn.toString()); 
    }

    @Test
    public void testLudoPlayerModelCoverage() {
        // Given
        LudoPlayer player = new LudoPlayer("u1", PlayerColor.GREEN);
        
        // When
        player.setBot(true);
        
        // Then
        Assert.assertEquals(player.getUserId(), "u1");
        Assert.assertEquals(player.getColor(), PlayerColor.GREEN);
        Assert.assertTrue(player.isBot());
        Assert.assertNotNull(player.toString());
    }

    @Test
    public void testLudoGameResultModelCoverage() {
        // Given
        LudoGameResult result = new LudoGameResult();
        
        // When
        result.setGameId("g1");
        result.setMaxPlayers(4);
        result.setWinnerId("w1");
        result.setPlayers(Map.of("p1", "P1"));
        result.setPlacement(Map.of("p1", 1));

        // Then
        Assert.assertEquals(result.getGameId(), "g1");
        Assert.assertEquals(result.getMaxPlayers(), 4);
        Assert.assertEquals(result.getWinnerId(), "w1");
        Assert.assertNotNull(result.getPlayers());
        Assert.assertNotNull(result.getPlacement());
        Assert.assertNotNull(result.toString()); 
    }

    @Test
    public void testLudoGameStateMessageCoverage() {
        // Given
        LudoGameStateMessage msg = new LudoGameStateMessage(
                "r1", RoomStatus.PLAYING, PlayerColor.RED, "p1", 
                6, true, true, 0, new ArrayList<>(), new HashMap<>(), null
        );

        // When
        msg.setRollsLeft(3);

        // Then
        Assert.assertEquals(msg.getGameId(), "r1");
        Assert.assertEquals(msg.getRollsLeft(), 3);
        Assert.assertNotNull(msg.toString()); 
        
        LudoGameStateMessage emptyMsg = new LudoGameStateMessage();
        Assert.assertNotNull(emptyMsg);
    }
}