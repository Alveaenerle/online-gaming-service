package com.online_games_service.ludo.model;

import com.online_games_service.common.enums.RoomStatus;
import com.online_games_service.ludo.dto.LudoGameStateMessage;
import com.online_games_service.ludo.dto.PlayerTimeoutMessage;
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
        // Given & When & Then - order follows enum: RED, BLUE, YELLOW, GREEN
        Assert.assertEquals(PlayerColor.RED.next(), PlayerColor.BLUE);
        Assert.assertEquals(PlayerColor.BLUE.next(), PlayerColor.YELLOW);
        Assert.assertEquals(PlayerColor.YELLOW.next(), PlayerColor.GREEN);
        Assert.assertEquals(PlayerColor.GREEN.next(), PlayerColor.RED);
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
        LudoPawn pawn = new LudoPawn(1, 13, PlayerColor.BLUE, 0, false, false);

        // When
        pawn.setPosition(18);
        pawn.setStepsMoved(5);
        pawn.setInHome(false);
        pawn.setInBase(false);

        // Then
        Assert.assertEquals(pawn.getId(), 1);
        Assert.assertEquals(pawn.getPosition(), 18);
        Assert.assertEquals(pawn.getColor(), PlayerColor.BLUE);
        Assert.assertFalse(pawn.isInHome());
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
                6, true, true, 0, new ArrayList<>(), new HashMap<>(), null, null,
                null, null, null
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

    // --- Tests for new bot filling and turn timer functionality ---

    @Test
    public void shouldFillBotsWhenFewerHumansThanMaxPlayers() {
        // Given
        String roomId = "room_bot_fill";
        List<String> playerIds = List.of("p1", "p2");
        String hostId = "p1";
        Map<String, String> usernames = Map.of("p1", "Player 1", "p2", "Player 2");
        int maxPlayers = 4;

        // When
        LudoGame game = new LudoGame(roomId, playerIds, hostId, usernames, maxPlayers);

        // Then
        Assert.assertEquals(game.getPlayers().size(), 4);
        Assert.assertEquals(game.getMaxPlayers(), 4);

        // First 2 should be humans
        Assert.assertFalse(game.getPlayers().get(0).isBot());
        Assert.assertFalse(game.getPlayers().get(1).isBot());

        // Last 2 should be bots
        Assert.assertTrue(game.getPlayers().get(2).isBot());
        Assert.assertTrue(game.getPlayers().get(3).isBot());

        // Verify bot usernames were added
        Map<String, String> finalUsernames = game.getPlayersUsernames();
        Assert.assertTrue(finalUsernames.containsKey("bot-1"));
        Assert.assertTrue(finalUsernames.containsKey("bot-2"));
        Assert.assertEquals(finalUsernames.get("bot-1"), "Bot 1");
        Assert.assertEquals(finalUsernames.get("bot-2"), "Bot 2");

        // Verify bot avatars were set
        Map<String, String> avatars = game.getPlayersAvatars();
        Assert.assertEquals(avatars.get("bot-1"), "bot_avatar.svg");
        Assert.assertEquals(avatars.get("bot-2"), "bot_avatar.svg");
    }

    @Test
    public void shouldNotFillBotsWhenHumansEqualMaxPlayers() {
        // Given
        String roomId = "room_full";
        List<String> playerIds = List.of("p1", "p2", "p3", "p4");
        String hostId = "p1";
        Map<String, String> usernames = Map.of("p1", "Player 1", "p2", "Player 2", "p3", "Player 3", "p4", "Player 4");
        int maxPlayers = 4;

        // When
        LudoGame game = new LudoGame(roomId, playerIds, hostId, usernames, maxPlayers);

        // Then
        Assert.assertEquals(game.getPlayers().size(), 4);
        for (LudoPlayer player : game.getPlayers()) {
            Assert.assertFalse(player.isBot());
        }
    }

    @Test
    public void legacyConstructorShouldUsePlayerIdsSizeAsMaxPlayers() {
        // Given
        String roomId = "room_legacy";
        List<String> playerIds = List.of("p1", "p2");
        String hostId = "p1";
        Map<String, String> usernames = Map.of("p1", "Player 1", "p2", "Player 2");

        // When
        LudoGame game = new LudoGame(roomId, playerIds, hostId, usernames);

        // Then
        Assert.assertEquals(game.getPlayers().size(), 2);
        Assert.assertEquals(game.getMaxPlayers(), 2);
        // No bots should be added
        for (LudoPlayer player : game.getPlayers()) {
            Assert.assertFalse(player.isBot());
        }
    }

    @Test
    public void isTurnExpired_shouldReturnFalseWhenTurnStartTimeIsNull() {
        // Given
        LudoGame game = new LudoGame("r1", List.of("p1"), "p1", null);
        game.setTurnStartTime(null);

        // When & Then
        Assert.assertFalse(game.isTurnExpired());
    }

    @Test
    public void isTurnExpired_shouldReturnFalseWhenUnder60Seconds() {
        // Given
        LudoGame game = new LudoGame("r1", List.of("p1"), "p1", null);
        game.setTurnStartTime(System.currentTimeMillis() - 30_000); // 30 seconds ago

        // When & Then
        Assert.assertFalse(game.isTurnExpired());
    }

    @Test
    public void isTurnExpired_shouldReturnTrueWhenOver60Seconds() {
        // Given
        LudoGame game = new LudoGame("r1", List.of("p1"), "p1", null);
        game.setTurnStartTime(System.currentTimeMillis() - 61_000); // 61 seconds ago

        // When & Then
        Assert.assertTrue(game.isTurnExpired());
    }

    @Test
    public void isTurnExpired_shouldReturnTrueAtExactly60Seconds() {
        // Given
        LudoGame game = new LudoGame("r1", List.of("p1"), "p1", null);
        game.setTurnStartTime(System.currentTimeMillis() - 60_000); // Exactly 60 seconds ago

        // When & Then
        Assert.assertTrue(game.isTurnExpired());
    }

    @Test
    public void getTurnRemainingSeconds_shouldReturnNullWhenTurnStartTimeIsNull() {
        // Given
        LudoGame game = new LudoGame("r1", List.of("p1"), "p1", null);
        game.setTurnStartTime(null);

        // When & Then
        Assert.assertNull(game.getTurnRemainingSeconds());
    }

    @Test
    public void getTurnRemainingSeconds_shouldReturn30WhenHalfTimeElapsed() {
        // Given
        LudoGame game = new LudoGame("r1", List.of("p1"), "p1", null);
        game.setTurnStartTime(System.currentTimeMillis() - 30_000); // 30 seconds ago

        // When
        Integer remaining = game.getTurnRemainingSeconds();

        // Then
        Assert.assertNotNull(remaining);
        Assert.assertTrue(remaining >= 29 && remaining <= 31); // Allow some tolerance for test execution time
    }

    @Test
    public void getTurnRemainingSeconds_shouldReturnZeroWhenExpired() {
        // Given
        LudoGame game = new LudoGame("r1", List.of("p1"), "p1", null);
        game.setTurnStartTime(System.currentTimeMillis() - 70_000); // 70 seconds ago

        // When
        Integer remaining = game.getTurnRemainingSeconds();

        // Then
        Assert.assertNotNull(remaining);
        Assert.assertEquals(remaining.intValue(), 0);
    }

    @Test
    public void shouldAssignCorrectColorsToPlayers() {
        // Given
        List<String> playerIds = List.of("p1", "p2", "p3", "p4");
        Map<String, String> usernames = new HashMap<>();
        playerIds.forEach(id -> usernames.put(id, "User " + id));

        // When
        LudoGame game = new LudoGame("r1", playerIds, "p1", usernames, 4);

        // Then - colors assigned in enum order: RED, BLUE, YELLOW, GREEN
        Assert.assertEquals(game.getPlayers().get(0).getColor(), PlayerColor.RED);
        Assert.assertEquals(game.getPlayers().get(1).getColor(), PlayerColor.BLUE);
        Assert.assertEquals(game.getPlayers().get(2).getColor(), PlayerColor.YELLOW);
        Assert.assertEquals(game.getPlayers().get(3).getColor(), PlayerColor.GREEN);
    }

    @Test
    public void botsShouldGetCorrectColorsAfterHumans() {
        // Given
        List<String> playerIds = List.of("p1");
        Map<String, String> usernames = Map.of("p1", "Player 1");

        // When
        LudoGame game = new LudoGame("r1", playerIds, "p1", usernames, 3);

        // Then - colors assigned in enum order: RED, BLUE, YELLOW, GREEN
        Assert.assertEquals(game.getPlayers().size(), 3);
        Assert.assertEquals(game.getPlayers().get(0).getColor(), PlayerColor.RED);
        Assert.assertEquals(game.getPlayers().get(1).getColor(), PlayerColor.BLUE);
        Assert.assertEquals(game.getPlayers().get(2).getColor(), PlayerColor.YELLOW);

        // First is human, rest are bots
        Assert.assertFalse(game.getPlayers().get(0).isBot());
        Assert.assertTrue(game.getPlayers().get(1).isBot());
        Assert.assertTrue(game.getPlayers().get(2).isBot());
    }

    @Test
    public void testPlayerTimeoutMessageCoverage() {
        // Given
        PlayerTimeoutMessage msg = new PlayerTimeoutMessage(
                "room-1",
                "player-1",
                "bot-1",
                "You have been replaced by a bot due to inactivity."
        );

        // Then
        Assert.assertEquals(msg.getRoomId(), "room-1");
        Assert.assertEquals(msg.getPlayerId(), "player-1");
        Assert.assertEquals(msg.getReplacedByBotId(), "bot-1");
        Assert.assertEquals(msg.getMessage(), "You have been replaced by a bot due to inactivity.");
        Assert.assertEquals(msg.getType(), "PLAYER_TIMEOUT");
        Assert.assertNotNull(msg.toString());

        // Test no-args constructor
        PlayerTimeoutMessage emptyMsg = new PlayerTimeoutMessage();
        Assert.assertNotNull(emptyMsg);
        Assert.assertEquals(emptyMsg.getType(), "PLAYER_TIMEOUT");

        // Test setters
        emptyMsg.setRoomId("new-room");
        emptyMsg.setPlayerId("new-player");
        emptyMsg.setReplacedByBotId("new-bot");
        emptyMsg.setMessage("New message");

        Assert.assertEquals(emptyMsg.getRoomId(), "new-room");
        Assert.assertEquals(emptyMsg.getPlayerId(), "new-player");
        Assert.assertEquals(emptyMsg.getReplacedByBotId(), "new-bot");
        Assert.assertEquals(emptyMsg.getMessage(), "New message");
    }

    @Test
    public void constructorWithMaxPlayersShouldHandleEmptyUsernames() {
        // Given
        List<String> playerIds = List.of("p1", "p2");

        // When
        LudoGame game = new LudoGame("r1", playerIds, "p1", null, 4);

        // Then
        Assert.assertEquals(game.getPlayers().size(), 4);
        Assert.assertTrue(game.getPlayersUsernames().containsKey("bot-1"));
        Assert.assertTrue(game.getPlayersUsernames().containsKey("bot-2"));
    }
}