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

        // Then (Verification of Getters/Lombok Data)
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
    }
}