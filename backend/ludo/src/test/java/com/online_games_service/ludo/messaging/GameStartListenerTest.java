package com.online_games_service.ludo.messaging;

import com.online_games_service.common.enums.GameType;
import com.online_games_service.common.messaging.GameStartMessage;
import com.online_games_service.ludo.repository.redis.LudoGameRedisRepository;
import com.online_games_service.ludo.service.LudoService;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class GameStartListenerTest {

    @Mock
    private LudoGameRedisRepository repository;

    @Mock
    private LudoService ludoService;

    private GameStartListener listener;
    private AutoCloseable mocks;

    @BeforeMethod
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        listener = new GameStartListener(repository, ludoService);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    public void shouldCreateGameWhenNotExists() {
        // Given
        GameStartMessage msg = new GameStartMessage(
                "room-1", "Room", GameType.LUDO,
                Map.of("p1", "P1", "p2", "P2"),
                Map.of("p1", "avatar_1.png", "p2", "avatar_2.png"),
                4, "p1", "P1"
        );

        when(repository.existsById("room-1")).thenReturn(false);

        // When
        listener.handleGameStart(msg);

        // Then
        verify(ludoService).createGame(
                eq("room-1"),
                any(),
                eq("p1"),
                any(),
                any(),
                eq(4)
        );
    }

    @Test
    public void shouldSkipWhenGameExists() {
        // Given
        GameStartMessage msg = new GameStartMessage("room-1", "Room", GameType.LUDO, Map.of(), Map.of(), 2, "p1", "P1");
        when(repository.existsById("room-1")).thenReturn(true);

        // When
        listener.handleGameStart(msg);

        // Then
        verify(ludoService, never()).createGame(anyString(), any(), anyString(), any(), any(), anyInt());
    }

    @Test
    public void shouldHandleNullMessage() {
        // When
        listener.handleGameStart(null);

        // Then
        verify(repository, never()).existsById(anyString());
    }

    @Test
    public void shouldPassMaxPlayersFromMessage() {
        // Given
        GameStartMessage msg = new GameStartMessage(
                "room-2", "Room 2", GameType.LUDO,
                Map.of("p1", "P1"),
                Map.of("p1", "avatar_1.png"),
                4, "p1", "P1"
        );

        when(repository.existsById("room-2")).thenReturn(false);

        // When
        listener.handleGameStart(msg);

        // Then
        verify(ludoService).createGame(
                eq("room-2"),
                any(),
                eq("p1"),
                any(),
                any(),
                eq(4)  // maxPlayers should be passed
        );
    }
}