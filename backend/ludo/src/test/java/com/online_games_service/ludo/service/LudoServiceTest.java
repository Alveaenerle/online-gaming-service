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
    @Mock private ScheduledExecutorService scheduler;

    private LudoService ludoService;
    private AutoCloseable mocks;

    @BeforeMethod
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        ludoService = new LudoService(gameRepository, gameResultRepository, rabbitTemplate, messagingTemplate);
        
        ReflectionTestUtils.setField(ludoService, "turnTimeoutScheduler", scheduler);
        
        doAnswer(invocation -> mock(ScheduledFuture.class))
            .when(scheduler).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));

        ReflectionTestUtils.setField(ludoService, "exchangeName", "game.events");
        ReflectionTestUtils.setField(ludoService, "finishRoutingKey", "ludo.finish");
    }

    @AfterMethod
    public void tearDown() throws Exception {
        if (mocks != null) mocks.close();
    }

    @Test
    public void shutdown_shouldCancelTasksAndShutdownScheduler() {
        // Given
        LudoGame game = createGame("r1", "p1", "p2");
        when(gameRepository.findById("r1")).thenReturn(Optional.of(game));
        when(gameRepository.save(any())).thenReturn(game);
        
        ludoService.rollDice("r1", "p1");

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
        ludoService.createGame(roomId, List.of("p1", "p2"), "p1", Map.of("p1", "A", "p2", "B"));

        // Then
        verify(gameRepository).createGameIfAbsent(argThat(g -> 
            g.getRoomId().equals(roomId) && 
            g.getPlayers().size() == 2 &&
            g.getCurrentPlayerColor() == PlayerColor.RED
        ));
        
        verify(messagingTemplate).convertAndSend(eq("/topic/game/r1"), any(Object.class));
        verify(scheduler).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
    }
    
    @Test
    public void createGame_shouldSkipIfGameExists() {
        // Given
        when(gameRepository.createGameIfAbsent(any(LudoGame.class))).thenReturn(false);
        
        // When
        ludoService.createGame("r1", List.of("p1"), "p1", null);
        
        // Then
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
        verify(scheduler, never()).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
    }

    @Test
    public void createGame_shouldNotFailIfPlayerListIsEmpty() {
        // Given & When
        ludoService.createGame("r1", Collections.emptyList(), "host", null);
        ludoService.createGame("r1", null, "host", null);

        // Then
        verify(gameRepository, never()).createGameIfAbsent(any());
    }

    @Test
    public void rollDice_shouldUpdateGameState() {
        // Given
        LudoGame game = createGame("r1", "p1", "p2");
        game.getPlayers().get(0).getPawns().get(0).setInBase(false);
        game.getPlayers().get(0).getPawns().get(0).setPosition(1);
        when(gameRepository.findById("r1")).thenReturn(Optional.of(game));

        // When
        ludoService.rollDice("r1", "p1");

        // Then
        Assert.assertTrue(game.isDiceRolled());
        Assert.assertTrue(game.getLastDiceRoll() >= 1 && game.getLastDiceRoll() <= 6);
    }

    @Test
    public void rollDice_shouldThrowIfNotPlayerTurn() {
        // Given
        LudoGame game = createGame("r1", "p1", "p2");
        game.setActivePlayerId("p2"); 
        when(gameRepository.findById("r1")).thenReturn(Optional.of(game));

        // When & Then
        Assert.assertThrows(GameLogicException.class, () -> ludoService.rollDice("r1", "p1"));
    }
    
    @Test
    public void rollDice_shouldThrowIfWaitingForMove() {
        // Given
        LudoGame game = createGame("r1", "p1", "p2");
        game.setDiceRolled(true);
        game.setWaitingForMove(true);
        when(gameRepository.findById("r1")).thenReturn(Optional.of(game));

        // When & Then
        Assert.assertThrows(GameLogicException.class, () -> ludoService.rollDice("r1", "p1"));
    }

    @Test
    public void rollDice_shouldThrowIfNoRollsLeft() {
        // Given
        LudoGame game = createGame("r1", "p1", "p2");
        game.setRollsLeft(0);
        when(gameRepository.findById("r1")).thenReturn(Optional.of(game));

        // When & Then
        Assert.assertThrows(IllegalStateException.class, () -> ludoService.rollDice("r1", "p1"));
    }

    @Test
    public void movePawn_shouldRequireSixToLeaveBase() {
        // Given
        LudoGame game = createGame("r1", "p1", "p2");
        game.setDiceRolled(true);
        game.setLastDiceRoll(3); 
        game.setWaitingForMove(true);
        when(gameRepository.findById("r1")).thenReturn(Optional.of(game));

        // When & Then
        Assert.assertThrows(InvalidMoveException.class, () -> ludoService.movePawn("r1", "p1", 0));
    }

    @Test
    public void movePawn_shouldLeaveBaseOnSix() {
        // Given
        LudoGame game = createGame("r1", "p1", "p2");
        game.setDiceRolled(true);
        game.setLastDiceRoll(6);
        game.setWaitingForMove(true);
        when(gameRepository.findById("r1")).thenReturn(Optional.of(game));

        // When
        ludoService.movePawn("r1", "p1", 0);

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
        LudoGame game = createGame("r1", "p1", "p2");
        LudoPlayer red = game.getPlayers().get(0);
        LudoPlayer blue = game.getPlayers().get(1);

        red.getPawns().get(0).setInBase(false);
        red.getPawns().get(0).setPosition(10);
        
        blue.getPawns().get(0).setInBase(false);
        blue.getPawns().get(0).setPosition(13);

        game.setDiceRolled(true);
        game.setLastDiceRoll(3);
        game.setWaitingForMove(true);
        
        when(gameRepository.findById("r1")).thenReturn(Optional.of(game));

        // When
        ludoService.movePawn("r1", "p1", 0);

        // Then
        Assert.assertEquals(red.getPawns().get(0).getPosition(), 13);
        Assert.assertTrue(blue.getPawns().get(0).isInBase());
    }
    
    @Test
    public void movePawn_shouldThrowIfPlayerNotFound() {
        // Given
        LudoGame game = createGame("r1", "p1", "p2");
        game.setDiceRolled(true);
        game.setActivePlayerId("ghost"); 
        when(gameRepository.findById("r1")).thenReturn(Optional.of(game));

        // When & Then
        Assert.assertThrows(IllegalArgumentException.class, () -> ludoService.movePawn("r1", "ghost", 0));
    }
    
    @Test
    public void movePawn_shouldThrowForInvalidIndex() {
        // Given
        LudoGame game = createGame("r1", "p1", "p2");
        game.setDiceRolled(true);
        when(gameRepository.findById("r1")).thenReturn(Optional.of(game));

        // When & Then
        Assert.assertThrows(InvalidMoveException.class, () -> ludoService.movePawn("r1", "p1", 99));
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
        verify(messagingTemplate, atLeastOnce()).convertAndSend(anyString(), any(Object.class));
        verify(scheduler, atLeastOnce()).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
    }

    @Test
    public void checkWinCondition_shouldFinishGame() {
        // Given
        LudoGame game = createGame("r1", "p1", "p2");
        LudoPlayer winner = game.getPlayers().get(0);
        
        for (LudoPawn p : winner.getPawns()) {
            p.setInBase(false);
            p.setInHome(true);
        }
        
        winner.getPawns().get(3).setInHome(false); 
        winner.getPawns().get(3).setStepsMoved(39); 
        winner.getPawns().get(3).setPosition(39); 
        
        game.setDiceRolled(true);
        game.setLastDiceRoll(1);
        game.setWaitingForMove(true);
        
        when(gameRepository.findById("r1")).thenReturn(Optional.of(game));

        // When
        ludoService.movePawn("r1", "p1", 3);

        // Then
        Assert.assertEquals(game.getStatus(), RoomStatus.FINISHED);
        Assert.assertEquals(game.getWinnerId(), "p1");
        
        verify(gameResultRepository).save(any());
        verify(rabbitTemplate).convertAndSend(eq("game.events"), eq("ludo.finish"), any(Object.class));
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
        LudoGame game = createGame("r1", "p1", "p2");
        LudoPlayer player = game.getPlayers().get(0);
        
        player.getPawns().get(0).setInBase(false);
        player.getPawns().get(0).setPosition(10);
        player.getPawns().get(0).setStepsMoved(10);

        player.getPawns().get(1).setInBase(false);
        player.getPawns().get(1).setPosition(12);
        
        game.setDiceRolled(true);
        game.setLastDiceRoll(2);
        game.setWaitingForMove(true);
        
        when(gameRepository.findById("r1")).thenReturn(Optional.of(game));

        // When & Then
        Assert.assertThrows(InvalidMoveException.class, () -> ludoService.movePawn("r1", "p1", 0));
    }

    @Test
    public void movePawn_shouldThrowIfTargetIsSafePointWithOpponent() {
        // Given
        LudoGame game = createGame("r1", "p1", "p2");
        LudoPlayer red = game.getPlayers().get(0); 
        LudoPlayer blue = game.getPlayers().get(1); 

        red.getPawns().get(0).setInBase(false);
        red.getPawns().get(0).setPosition(8); 
        
        blue.getPawns().get(0).setInBase(false);
        blue.getPawns().get(0).setPosition(10); 

        game.setDiceRolled(true);
        game.setLastDiceRoll(2);
        game.setWaitingForMove(true);
        
        when(gameRepository.findById("r1")).thenReturn(Optional.of(game));

        // When & Then
        Assert.assertThrows(InvalidMoveException.class, () -> ludoService.movePawn("r1", "p1", 0));
    }

    @Test
    public void rollDice_shouldPassTurnIfNoMovesPossible() {        
        // Given
        LudoGame game = createGame("r1", "p1", "p2");
        game.setRollsLeft(1); 
        
        game.getPlayers().get(0).getPawns().get(0).setInBase(false);
        game.getPlayers().get(0).getPawns().get(0).setPosition(1);
        
        game.getPlayers().get(0).getPawns().get(1).setInBase(false);
        game.getPlayers().get(0).getPawns().get(1).setPosition(3); 

        game.setDiceRolled(true);
        game.setLastDiceRoll(2);
        game.setWaitingForMove(true); 
        
        when(gameRepository.findById("r1")).thenReturn(Optional.of(game));
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
        LudoGame game = createGame(roomId, "p1", "p2");
        
        LudoPlayer bot = new LudoPlayer(botId, PlayerColor.RED);
        bot.setBot(true);
        bot.getPawns().get(0).setInBase(false); 
        bot.getPawns().get(0).setPosition(5); 
        
        game.setPlayers(List.of(bot));
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

    private LudoGame createGame(String roomId, String p1, String p2) {
        return new LudoGame(roomId, List.of(p1, p2), p1, Map.of(p1, "User1", p2, "User2"));
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
}