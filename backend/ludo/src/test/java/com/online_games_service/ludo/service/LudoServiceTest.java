package com.online_games_service.ludo.service;

import com.online_games_service.common.enums.RoomStatus;
import com.online_games_service.ludo.enums.PlayerColor;
import com.online_games_service.ludo.exception.GameLogicException;
import com.online_games_service.ludo.exception.InvalidMoveException;
import com.online_games_service.ludo.model.LudoGame;
import com.online_games_service.ludo.model.LudoPawn;
import com.online_games_service.ludo.model.LudoPlayer;
import com.online_games_service.ludo.repository.mongo.LudoGameResultRepository;
import com.online_games_service.ludo.repository.redis.LudoGameRedisRepository;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class LudoServiceTest {

    @Mock private LudoGameRedisRepository gameRepository;
    @Mock private LudoGameResultRepository gameResultRepository;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private ValueOperations<String, String> stringValueOperations;
    @Mock private ScheduledExecutorService scheduler;

    private LudoService ludoService;
    private AutoCloseable mocks;
    private final String USER_GAME_PREFIX = "ludo:user-game:";

    @BeforeMethod
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        when(stringRedisTemplate.opsForValue()).thenReturn(stringValueOperations);

        ludoService = new LudoService(gameRepository, gameResultRepository, rabbitTemplate, messagingTemplate, stringRedisTemplate);

        ReflectionTestUtils.setField(ludoService, "turnTimeoutScheduler", scheduler);

        doAnswer(invocation -> mock(ScheduledFuture.class))
            .when(scheduler).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));

        ReflectionTestUtils.setField(ludoService, "exchangeName", "game.events");
        ReflectionTestUtils.setField(ludoService, "finishRoutingKey", "ludo.finish");
        ReflectionTestUtils.setField(ludoService, "leaveRoutingKey", "player.leave");
    }

    @AfterMethod
    public void tearDown() throws Exception {
        if (mocks != null) mocks.close();
    }

    @Test
    public void shutdown_shouldCancelTasksAndShutdownScheduler() {
        // Given
        String userId = "p1";
        String roomId = "r1";

        LudoGame game = createGame(roomId, userId, "p2");

        when(stringValueOperations.get(USER_GAME_PREFIX + userId)).thenReturn(roomId);
        when(gameRepository.findById(roomId)).thenReturn(Optional.of(game));
        when(gameRepository.save(any())).thenReturn(game);

        ludoService.rollDice(userId);

        // When
        ludoService.shutdown();

        // Then
        verify(scheduler, atLeastOnce()).shutdown();
        verify(scheduler, atLeastOnce()).shutdownNow();
    }

    @Test
    public void createGame_shouldInitializeAndSaveAtomically() {
        // Given
        String roomId = "r1";
        when(gameRepository.createGameIfAbsent(any(LudoGame.class))).thenReturn(true);

        // When
        ludoService.createGame(roomId, List.of("p1", "p2"), "p1", Map.of("p1", "A", "p2", "B"), Map.of(), 2);

        // Then
        verify(gameRepository).createGameIfAbsent(argThat(g ->
            g.getRoomId().equals(roomId) &&
            g.getPlayers().size() == 2 &&
            g.getCurrentPlayerColor() == PlayerColor.RED
        ));

        verify(stringValueOperations).set(eq(USER_GAME_PREFIX + "p1"), eq(roomId), any());
        verify(stringValueOperations).set(eq(USER_GAME_PREFIX + "p2"), eq(roomId), any());

        // Verify that state is sent to each human player's personal topic
        verify(messagingTemplate).convertAndSend(eq("/topic/ludo/p1"), any(Object.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/ludo/p2"), any(Object.class));
        verify(scheduler).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
    }

    @Test
    public void createGame_shouldSkipIfGameExists() {
        // Given
        when(gameRepository.createGameIfAbsent(any(LudoGame.class))).thenReturn(false);

        // When
        ludoService.createGame("r1", List.of("p1"), "p1", null, null, 2);

        // Then
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
        verify(stringValueOperations, never()).set(anyString(), anyString(), any());
    }

    @Test
    public void createGame_shouldNotFailIfPlayerListIsEmpty() {
        // Given & When
        ludoService.createGame("r1", Collections.emptyList(), "host", null, null, 2);
        ludoService.createGame("r1", null, "host", null, null, 2);

        // Then
        verify(gameRepository, never()).createGameIfAbsent(any());
    }

    @Test
    public void createGame_shouldFillBotsWhenFewerHumansThanMaxPlayers() {
        // Given
        String roomId = "r1";
        when(gameRepository.createGameIfAbsent(any(LudoGame.class))).thenReturn(true);

        // When
        ludoService.createGame(roomId, List.of("p1"), "p1", Map.of("p1", "Player1"), Map.of(), 4);

        // Then
        verify(gameRepository).createGameIfAbsent(argThat(g ->
            g.getPlayers().size() == 4 &&
            g.getPlayers().stream().filter(LudoPlayer::isBot).count() == 3
        ));
    }

    @Test
    public void rollDice_shouldUpdateGameState() {
        // Given
        String userId = "p1";
        String roomId = "r1";
        LudoGame game = createGame(roomId, userId, "p2");

        game.getPlayers().get(0).getPawns().get(0).setInBase(false);
        game.getPlayers().get(0).getPawns().get(0).setPosition(1);

        when(stringValueOperations.get(USER_GAME_PREFIX + userId)).thenReturn(roomId);
        when(gameRepository.findById(roomId)).thenReturn(Optional.of(game));

        // When
        ludoService.rollDice(userId);

        // Then
        Assert.assertTrue(game.isDiceRolled());
        Assert.assertTrue(game.getLastDiceRoll() >= 1 && game.getLastDiceRoll() <= 6);
    }

    @Test
    public void rollDice_shouldThrowIfNotPlayerTurn() {
        // Given
        String userId = "p1";
        String roomId = "r1";
        LudoGame game = createGame(roomId, userId, "p2");
        game.setActivePlayerId("p2");

        when(stringValueOperations.get(USER_GAME_PREFIX + userId)).thenReturn(roomId);
        when(gameRepository.findById(roomId)).thenReturn(Optional.of(game));

        // When & Then
        Assert.assertThrows(GameLogicException.class, () -> ludoService.rollDice(userId));
    }

    @Test
    public void rollDice_shouldThrowIfWaitingForMove() {
        // Given
        String userId = "p1";
        String roomId = "r1";
        LudoGame game = createGame(roomId, userId, "p2");
        game.setDiceRolled(true);
        game.setWaitingForMove(true);

        when(stringValueOperations.get(USER_GAME_PREFIX + userId)).thenReturn(roomId);
        when(gameRepository.findById(roomId)).thenReturn(Optional.of(game));

        // When & Then
        Assert.assertThrows(GameLogicException.class, () -> ludoService.rollDice(userId));
    }

    @Test
    public void rollDice_shouldThrowIfNoRollsLeft() {
        // Given
        String userId = "p1";
        String roomId = "r1";
        LudoGame game = createGame(roomId, userId, "p2");
        game.setRollsLeft(0);

        when(stringValueOperations.get(USER_GAME_PREFIX + userId)).thenReturn(roomId);
        when(gameRepository.findById(roomId)).thenReturn(Optional.of(game));

        // When & Then
        Assert.assertThrows(IllegalStateException.class, () -> ludoService.rollDice(userId));
    }

    @Test
    public void movePawn_shouldRequireSixToLeaveBase() {
        // Given
        String userId = "p1";
        String roomId = "r1";
        LudoGame game = createGame(roomId, userId, "p2");
        game.setDiceRolled(true);
        game.setLastDiceRoll(3);
        game.setWaitingForMove(true);

        when(stringValueOperations.get(USER_GAME_PREFIX + userId)).thenReturn(roomId);
        when(gameRepository.findById(roomId)).thenReturn(Optional.of(game));

        // When & Then
        Assert.assertThrows(InvalidMoveException.class, () -> ludoService.movePawn(userId, 0));
    }

    @Test
    public void movePawn_shouldLeaveBaseOnSix() {
        // Given
        String userId = "p1";
        String roomId = "r1";
        LudoGame game = createGame(roomId, userId, "p2");
        game.setDiceRolled(true);
        game.setLastDiceRoll(6);
        game.setWaitingForMove(true);

        when(stringValueOperations.get(USER_GAME_PREFIX + userId)).thenReturn(roomId);
        when(gameRepository.findById(roomId)).thenReturn(Optional.of(game));

        // When
        ludoService.movePawn(userId, 0);

        // Then
        LudoPawn pawn = game.getPlayers().get(0).getPawns().get(0);
        Assert.assertFalse(pawn.isInBase());
        Assert.assertEquals(pawn.getPosition(), PlayerColor.RED.getStartPosition());
        Assert.assertFalse(game.isDiceRolled());
        Assert.assertEquals(game.getRollsLeft(), 1);
    }

    @Test
    public void movePawn_shouldCaptureOpponent() {
        // Given
        String userId = "p1";
        String roomId = "r1";
        LudoGame game = createGame(roomId, userId, "p2");
        LudoPlayer red = game.getPlayers().get(0);
        LudoPlayer blue = game.getPlayers().get(1);

        red.getPawns().get(0).setInBase(false);
        red.getPawns().get(0).setPosition(14);

        blue.getPawns().get(0).setInBase(false);
        blue.getPawns().get(0).setPosition(17);

        game.setDiceRolled(true);
        game.setLastDiceRoll(3);
        game.setWaitingForMove(true);

        when(stringValueOperations.get(USER_GAME_PREFIX + userId)).thenReturn(roomId);
        when(gameRepository.findById(roomId)).thenReturn(Optional.of(game));

        // When
        ludoService.movePawn(userId, 0);

        // Then
        Assert.assertEquals(red.getPawns().get(0).getPosition(), 17);
        Assert.assertTrue(blue.getPawns().get(0).isInBase());
    }

    @Test
    public void movePawn_shouldThrowIfPlayerNotFound() {
        // Given
        String userId = "ghost";
        String roomId = "r1";
        LudoGame game = createGame(roomId, "p1", "p2");
        game.setDiceRolled(true);
        game.setActivePlayerId(userId);

        when(stringValueOperations.get(USER_GAME_PREFIX + userId)).thenReturn(roomId);
        when(gameRepository.findById(roomId)).thenReturn(Optional.of(game));

        // When & Then
        Assert.assertThrows(IllegalArgumentException.class, () -> ludoService.movePawn(userId, 0));
    }

    @Test
    public void movePawn_shouldThrowForInvalidIndex() {
        // Given
        String userId = "p1";
        String roomId = "r1";
        LudoGame game = createGame(roomId, userId, "p2");
        game.setDiceRolled(true);

        when(stringValueOperations.get(USER_GAME_PREFIX + userId)).thenReturn(roomId);
        when(gameRepository.findById(roomId)).thenReturn(Optional.of(game));

        // When & Then
        Assert.assertThrows(InvalidMoveException.class, () -> ludoService.movePawn(userId, 99));
    }

    @Test
    public void handleTurnTimeout_shouldReplaceWithBot() {
        // Given
        LudoGame game = createGame("r1", "p1", "p2");
        when(gameRepository.findById("r1")).thenReturn(Optional.of(game));
        when(gameRepository.save(any(LudoGame.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        ReflectionTestUtils.invokeMethod(ludoService, "handleTurnTimeout", "r1", "p1");

        // Then
        LudoPlayer p1 = game.getPlayers().get(0);
        Assert.assertTrue(p1.isBot());
        Assert.assertTrue(p1.getUserId().startsWith("bot-"));

        verify(stringRedisTemplate).delete(USER_GAME_PREFIX + "p1");

        verify(messagingTemplate, atLeastOnce()).convertAndSend(anyString(), any(Object.class));
        verify(scheduler, atLeastOnce()).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
    }

    @Test
    public void checkWinCondition_shouldFinishGame() {
        // Given
        String userId = "p1";
        String roomId = "r1";
        LudoGame game = createGame(roomId, userId, "p2");
        LudoPlayer winner = game.getPlayers().get(0);

        for (LudoPawn p : winner.getPawns()) {
            p.setInBase(false);
            p.setInHome(true);
        }

        winner.getPawns().get(3).setInHome(false);
        winner.getPawns().get(3).setStepsMoved(43);
        winner.getPawns().get(3).setPosition(43);

        game.setDiceRolled(true);
        game.setLastDiceRoll(1);
        game.setWaitingForMove(true);

        when(stringValueOperations.get(USER_GAME_PREFIX + userId)).thenReturn(roomId);
        when(gameRepository.findById(roomId)).thenReturn(Optional.of(game));

        // When
        ludoService.movePawn(userId, 3);

        // Then
        Assert.assertEquals(game.getStatus(), RoomStatus.FINISHED);
        Assert.assertEquals(game.getWinnerId(), userId);

        verify(gameResultRepository).save(any());
        verify(rabbitTemplate).convertAndSend(eq("game.events"), eq("ludo.finish"), any(Object.class));
        verify(stringRedisTemplate, atLeastOnce()).delete(anyString());
    }

    @Test
    public void handleTurnTimeout_shouldLogExceptionOnDatabaseError() {
        // Given
        LudoGame game = createGame("r1", "p1", "p2");
        when(gameRepository.findById("r1")).thenReturn(Optional.of(game));
        doThrow(new RuntimeException("DB Error")).when(gameRepository).save(any());

        // When
        ReflectionTestUtils.invokeMethod(ludoService, "handleTurnTimeout", "r1", "p1");

        // Then
        verify(gameRepository).save(any());
    }

    @Test
    public void movePawn_shouldThrowIfTargetFieldOccupiedBySelf() {
        // Given
        String userId = "p1";
        String roomId = "r1";
        LudoGame game = createGame(roomId, userId, "p2");
        LudoPlayer player = game.getPlayers().get(0);

        player.getPawns().get(0).setInBase(false);
        player.getPawns().get(0).setPosition(10);
        player.getPawns().get(0).setStepsMoved(10);

        player.getPawns().get(1).setInBase(false);
        player.getPawns().get(1).setPosition(12);

        game.setDiceRolled(true);
        game.setLastDiceRoll(2);
        game.setWaitingForMove(true);

        when(stringValueOperations.get(USER_GAME_PREFIX + userId)).thenReturn(roomId);
        when(gameRepository.findById(roomId)).thenReturn(Optional.of(game));

        // When & Then
        Assert.assertThrows(InvalidMoveException.class, () -> ludoService.movePawn(userId, 0));
    }

    @Test
    public void movePawn_shouldThrowIfTargetIsSafePointWithOpponent() {
        // Given
        String userId = "p1";
        String roomId = "r1";
        LudoGame game = createGame(roomId, userId, "p2");
        LudoPlayer red = game.getPlayers().get(0);
        LudoPlayer blue = game.getPlayers().get(1);

        red.getPawns().get(0).setInBase(false);
        red.getPawns().get(0).setPosition(9);

        blue.getPawns().get(0).setInBase(false);
        blue.getPawns().get(0).setPosition(11);

        game.setDiceRolled(true);
        game.setLastDiceRoll(2);
        game.setWaitingForMove(true);

        when(stringValueOperations.get(USER_GAME_PREFIX + userId)).thenReturn(roomId);
        when(gameRepository.findById(roomId)).thenReturn(Optional.of(game));

        // When & Then
        Assert.assertThrows(InvalidMoveException.class, () -> ludoService.movePawn(userId, 0));
    }

    @Test
    public void rollDice_shouldPassTurnIfNoMovesPossible() {
        // Given
        String userId = "p1";
        String roomId = "r1";
        LudoGame game = createGame(roomId, userId, "p2");
        game.setRollsLeft(1);

        game.getPlayers().get(0).getPawns().get(0).setInBase(false);
        game.getPlayers().get(0).getPawns().get(0).setPosition(1);

        game.getPlayers().get(0).getPawns().get(1).setInBase(false);
        game.getPlayers().get(0).getPawns().get(1).setPosition(3);

        game.setDiceRolled(true);
        game.setLastDiceRoll(2);
        game.setWaitingForMove(true);

        // When & Then
        when(stringValueOperations.get(USER_GAME_PREFIX + userId)).thenReturn(roomId);
        when(gameRepository.findById(roomId)).thenReturn(Optional.of(game));
    }

    @Test
    public void processBotStep_shouldInvokeLogic() {
        // Given
        String roomId = "bot-room";
        String botId = "bot-1";
        String humanId = "human-1";
        LudoGame game = createGame(roomId, "p1", "p2");

        LudoPlayer bot = new LudoPlayer(botId, PlayerColor.RED);
        bot.setBot(true);

        LudoPlayer human = new LudoPlayer(humanId, PlayerColor.BLUE);
        human.setBot(false);

        game.setPlayers(List.of(bot, human));
        game.setActivePlayerId(botId);

        when(gameRepository.findById(roomId)).thenReturn(Optional.of(game));

        // When
        ReflectionTestUtils.invokeMethod(ludoService, "processBotStep", roomId, botId);

        // Then
        verify(gameRepository, atLeastOnce()).save(any(LudoGame.class));
        verify(scheduler, atLeastOnce()).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
    }

    @Test
    public void executeBotMove_shouldMovePawn() {
        // Given
        String roomId = "bot-room-move";
        String botId = "bot-1";
        String humanId = "human-1";
        LudoGame game = createGame(roomId, "p1", "p2");

        LudoPlayer bot = new LudoPlayer(botId, PlayerColor.RED);
        bot.setBot(true);
        bot.getPawns().get(0).setInBase(false);
        bot.getPawns().get(0).setPosition(5);

        LudoPlayer human = new LudoPlayer(humanId, PlayerColor.BLUE);
        human.setBot(false);

        game.setPlayers(List.of(bot, human));
        game.setActivePlayerId(botId);
        game.setLastDiceRoll(3);
        game.setDiceRolled(true);

        when(gameRepository.findById(roomId)).thenReturn(Optional.of(game));

        // When
        ReflectionTestUtils.invokeMethod(ludoService, "executeBotMove", roomId, botId, 3);

        // Then
        verify(gameRepository, atLeastOnce()).save(any(LudoGame.class));
        Assert.assertEquals(bot.getPawns().get(0).getPosition(), 8);
    }

    @Test
    public void executeBotMove_shouldPassTurnIfNoMovePossible() {
        // Given
        String roomId = "bot-room-pass";
        String botId = "bot-1";
        String humanId = "human-1";
        LudoGame game = createGame(roomId, "p1", "p2");

        LudoPlayer bot = new LudoPlayer(botId, PlayerColor.RED);
        bot.setBot(true);

        LudoPlayer human = new LudoPlayer(humanId, PlayerColor.BLUE);
        human.setBot(false);

        game.setPlayers(List.of(bot, human));
        game.setActivePlayerId(botId);

        when(gameRepository.findById(roomId)).thenReturn(Optional.of(game));

        // When
        ReflectionTestUtils.invokeMethod(ludoService, "executeBotMove", roomId, botId, 3);

        // Then
        verify(gameRepository, atLeastOnce()).save(any(LudoGame.class));
    }

    @Test
    public void executeBotMove_shouldHandleExceptionGracefully() {
        // Given
        String roomId = "error-room";
        when(gameRepository.findById(roomId)).thenThrow(new RuntimeException("DB Error"));

        // When & Then
        ReflectionTestUtils.invokeMethod(ludoService, "executeBotMove", roomId, "bot-1", 6);
    }

    @Test
    public void shouldAbortGame_WhenLastHumanTimesOut() {
        // Given
        String roomId = "abandoned-room";
        LudoGame game = createGame(roomId, "p1", "p2");

        game.getPlayers().get(1).setBot(true);
        game.getPlayers().get(1).setUserId("bot-1");

        game.setActivePlayerId("p1");

        when(gameRepository.findById(roomId)).thenReturn(Optional.of(game));

        // When
        ReflectionTestUtils.invokeMethod(ludoService, "handleTurnTimeout", roomId, "p1");

        // Then
        verify(gameRepository).deleteById(roomId);
        verify(scheduler, never()).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
    }

    @Test
    public void canPawnMoveSimple_shouldReturnFalseIfStartBlocked() {
        // Given
        String roomId = "r1";
        LudoGame game = createGame(roomId, "p1", "p2");
        LudoPlayer player = game.getPlayers().get(0); // RED

        player.getPawns().get(0).setInBase(false);
        player.getPawns().get(0).setPosition(0);

        LudoPawn pawnInBase = player.getPawns().get(1);

        // When
        boolean canMove = ReflectionTestUtils.invokeMethod(ludoService, "canPawnMoveSimple", game, player, pawnInBase, 6);

        // Then
        Assert.assertFalse(canMove);
    }

    @Test
    public void canPlayerMove_shouldReturnFalseIfNoMovePossible() {
        // Given
        String roomId = "r1";
        LudoGame game = createGame(roomId, "p1", "p2");
        LudoPlayer player = game.getPlayers().get(0);

        // When
        boolean canMove = ReflectionTestUtils.invokeMethod(ludoService, "canPlayerMove", game, "p1", 5);

        // Then
        Assert.assertFalse(canMove);
    }

    @Test
    public void performMoveLogic_shouldEnterHomeCorrectly() {
        // Given
        String userId = "p1";
        String roomId = "r1";
        LudoGame game = createGame(roomId, userId, "p2");
        LudoPlayer player = game.getPlayers().get(0);
        LudoPawn pawn = player.getPawns().get(0);

        pawn.setInBase(false);
        pawn.setPosition(43);
        pawn.setStepsMoved(43);

        game.setDiceRolled(true);
        game.setLastDiceRoll(1);
        game.setWaitingForMove(true);

        when(stringValueOperations.get(USER_GAME_PREFIX + userId)).thenReturn(roomId);
        when(gameRepository.findById(roomId)).thenReturn(Optional.of(game));

        // When
        ludoService.movePawn(userId, 0);

        // Then
        Assert.assertTrue(pawn.isInHome());
        Assert.assertEquals(pawn.getPosition(), -2);
        Assert.assertEquals(pawn.getStepsMoved(), 47);
    }

    @Test
    public void handleTurnTimeout_shouldSendTimeoutNotification() {
        // Given
        LudoGame game = createGame("r1", "p1", "p2");
        when(gameRepository.findById("r1")).thenReturn(Optional.of(game));
        when(gameRepository.save(any(LudoGame.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        ReflectionTestUtils.invokeMethod(ludoService, "handleTurnTimeout", "r1", "p1");

        // Then
        verify(messagingTemplate).convertAndSend(
                eq("/topic/ludo/p1/timeout"),
                any(Object.class)
        );
    }

    @Test
    public void notifyPlayerTimeout_shouldNotSendForBots() {
        // Given - bot player
        String botId = "bot-1";

        // When
        ReflectionTestUtils.invokeMethod(ludoService, "notifyPlayerTimeout", botId, "r1", "bot-2");

        // Then - should not send any message for bot
        verify(messagingTemplate, never()).convertAndSend(contains("/timeout"), any(Object.class));
    }

    @Test
    public void notifyPlayerTimeout_shouldNotSendForNullPlayerId() {
        // When
        ReflectionTestUtils.invokeMethod(ludoService, "notifyPlayerTimeout", (String) null, "r1", "bot-1");

        // Then
        verify(messagingTemplate, never()).convertAndSend(contains("/timeout"), any(Object.class));
    }

    @Test
    public void handleTurnTimeout_shouldSkipIfGameNotFound() {
        // Given
        when(gameRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // When
        ReflectionTestUtils.invokeMethod(ludoService, "handleTurnTimeout", "nonexistent", "p1");

        // Then
        verify(gameRepository, never()).save(any());
        verify(messagingTemplate, never()).convertAndSend(contains("/timeout"), any(Object.class));
    }

    @Test
    public void handleTurnTimeout_shouldSkipIfGameNotPlaying() {
        // Given
        LudoGame game = createGame("r1", "p1", "p2");
        game.setStatus(RoomStatus.FINISHED);
        when(gameRepository.findById("r1")).thenReturn(Optional.of(game));

        // When
        ReflectionTestUtils.invokeMethod(ludoService, "handleTurnTimeout", "r1", "p1");

        // Then
        verify(gameRepository, never()).save(any());
    }

    @Test
    public void handleTurnTimeout_shouldSkipIfDifferentActivePlayer() {
        // Given
        LudoGame game = createGame("r1", "p1", "p2");
        game.setActivePlayerId("p2"); // Different player
        when(gameRepository.findById("r1")).thenReturn(Optional.of(game));

        // When
        ReflectionTestUtils.invokeMethod(ludoService, "handleTurnTimeout", "r1", "p1");

        // Then
        verify(gameRepository, never()).save(any());
    }

    @Test
    public void handlePlayerLeave_shouldReplaceWithBot() {
        // Given
        String userId = "p1";
        String roomId = "r1";
        LudoGame game = createGame(roomId, userId, "p2");

        when(stringValueOperations.get(USER_GAME_PREFIX + userId)).thenReturn(roomId);
        when(gameRepository.findById(roomId)).thenReturn(Optional.of(game));
        when(gameRepository.save(any(LudoGame.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        ludoService.handlePlayerLeave(userId);

        // Then
        LudoPlayer replacedPlayer = game.getPlayers().get(0);
        Assert.assertTrue(replacedPlayer.isBot());
        Assert.assertTrue(replacedPlayer.getUserId().startsWith("bot-"));

        verify(stringRedisTemplate).delete(USER_GAME_PREFIX + userId);
    }

    @Test
    public void handlePlayerLeave_shouldNotProcessBots() {
        // Given
        String botId = "bot-1";

        // When
        ludoService.handlePlayerLeave(botId);

        // Then
        verify(gameRepository, never()).findById(anyString());
    }

    @Test
    public void handlePlayerLeave_shouldHandlePlayerNotInGame() {
        // Given
        String userId = "unknown";
        when(stringValueOperations.get(USER_GAME_PREFIX + userId)).thenReturn(null);

        // When
        ludoService.handlePlayerLeave(userId);

        // Then
        verify(gameRepository, never()).findById(anyString());
    }

    @Test
    public void handlePlayerLeave_shouldTriggerBotTurnIfActivePlayer() {
        // Given
        String userId = "p1";
        String roomId = "r1";
        LudoGame game = createGame(roomId, userId, "p2");
        game.setActivePlayerId(userId); // The leaving player is active

        when(stringValueOperations.get(USER_GAME_PREFIX + userId)).thenReturn(roomId);
        when(gameRepository.findById(roomId)).thenReturn(Optional.of(game));
        when(gameRepository.save(any(LudoGame.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        ludoService.handlePlayerLeave(userId);

        // Then
        // Bot turn should be scheduled
        verify(scheduler, atLeastOnce()).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
    }

    @Test
    public void getGameState_shouldReturnMappedDTO() {
        // Given
        String userId = "p1";
        String roomId = "r1";
        LudoGame game = createGame(roomId, userId, "p2");

        when(stringValueOperations.get(USER_GAME_PREFIX + userId)).thenReturn(roomId);
        when(gameRepository.findById(roomId)).thenReturn(Optional.of(game));

        // When
        var state = ludoService.getGameState(userId);

        // Then
        Assert.assertNotNull(state);
        Assert.assertEquals(state.getGameId(), roomId);
    }

    @Test
    public void requestStateForUser_shouldBroadcastState() {
        // Given
        String userId = "p1";
        String roomId = "r1";
        LudoGame game = createGame(roomId, userId, "p2");

        when(stringValueOperations.get(USER_GAME_PREFIX + userId)).thenReturn(roomId);
        when(gameRepository.findById(roomId)).thenReturn(Optional.of(game));

        // When
        ludoService.requestStateForUser(userId);

        // Then - state is sent to each human player's personal topic
        verify(messagingTemplate).convertAndSend(eq("/topic/ludo/" + userId), any(Object.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/ludo/p2"), any(Object.class));
    }

    private LudoGame createGame(String roomId, String p1, String p2) {
        return new LudoGame(roomId, List.of(p1, p2), p1, Map.of(p1, "User1", p2, "User2"));
    }
}