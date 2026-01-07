package com.online_games_service.makao.messaging;

import com.online_games_service.common.enums.GameType;
import com.online_games_service.common.messaging.GameStartMessage;
import com.online_games_service.makao.model.MakaoGame;
import com.online_games_service.makao.repository.redis.MakaoGameRedisRepository;
import com.online_games_service.makao.service.MakaoGameService;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class GameStartListenerTest {

    @Mock
    private MakaoGameRedisRepository repository;

    @Mock
    private MakaoGameService makaoGameService;

    private AutoCloseable mocks;
    private GameStartListener listener;

    @BeforeMethod
    public void setup() {
        mocks = MockitoAnnotations.openMocks(this);
        listener = new GameStartListener(repository, makaoGameService);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    public void shouldCreateGameWhenNotExists() {
        GameStartMessage msg = new GameStartMessage(
                "room-1",
                "Room",
                GameType.MAKAO,
                Map.of("host", "Host", "p2", "P2"),
                4,
                "host",
                "Host"
        );

        when(repository.existsById("room-1")).thenReturn(false);
        when(repository.save(any(MakaoGame.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(makaoGameService).initializeGameAfterStart("room-1");

        listener.handleGameStart(msg);

        verify(repository).save(any(MakaoGame.class));
        verify(makaoGameService).initializeGameAfterStart("room-1");
    }

    @Test
    public void shouldSkipWhenAlreadyExists() {
        GameStartMessage msg = new GameStartMessage(
                "room-1",
                "Room",
                GameType.MAKAO,
                Map.of("host", "Host"),
                4,
                "host",
                "Host"
        );

        when(repository.existsById("room-1")).thenReturn(true);

        listener.handleGameStart(msg);

        verify(repository, never()).save(any());
        verify(makaoGameService, never()).initializeGameAfterStart(any());
    }

    @Test
    public void shouldSkipWhenRoomIdMissing() {
        GameStartMessage msg = new GameStartMessage(
                "",
                "Room",
                GameType.MAKAO,
                Map.of(),
                4,
                "host",
                "Host"
        );

        listener.handleGameStart(msg);

        verify(repository, never()).save(any());
        verify(makaoGameService, never()).initializeGameAfterStart(any());
    }
}
