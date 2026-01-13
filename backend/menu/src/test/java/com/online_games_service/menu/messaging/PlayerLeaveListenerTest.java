package com.online_games_service.menu.messaging;

import com.online_games_service.common.messaging.PlayerLeaveMessage;
import com.online_games_service.menu.service.GameRoomService;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class PlayerLeaveListenerTest {

    @Mock
    private GameRoomService gameRoomService;

    private PlayerLeaveListener listener;
    private AutoCloseable mocks;

    @BeforeMethod
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        listener = new PlayerLeaveListener(gameRoomService);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    public void handle_callsRemovePlayerFromRoom() {
        PlayerLeaveMessage message = new PlayerLeaveMessage(
                "room-123",
                "player-456",
                PlayerLeaveMessage.LeaveReason.VOLUNTARY);

        listener.handle(message);

        verify(gameRoomService).removePlayerFromRoom("room-123", "player-456");
    }

    @Test
    public void handle_worksWithTimeoutReason() {
        PlayerLeaveMessage message = new PlayerLeaveMessage(
                "room-timeout",
                "timed-out-player",
                PlayerLeaveMessage.LeaveReason.TIMEOUT);

        listener.handle(message);

        verify(gameRoomService).removePlayerFromRoom("room-timeout", "timed-out-player");
    }

    @Test
    public void handle_worksWithDisconnectReason() {
        PlayerLeaveMessage message = new PlayerLeaveMessage(
                "room-disconnect",
                "disconnected-player",
                PlayerLeaveMessage.LeaveReason.DISCONNECT);

        listener.handle(message);

        verify(gameRoomService).removePlayerFromRoom("room-disconnect", "disconnected-player");
    }

    @Test
    public void handle_ignoresNullMessage() {
        listener.handle(null);

        verifyNoInteractions(gameRoomService);
    }

    @Test
    public void handle_ignoresMessageWithNullRoomId() {
        PlayerLeaveMessage message = new PlayerLeaveMessage(
                null,
                "player-456",
                PlayerLeaveMessage.LeaveReason.VOLUNTARY);

        listener.handle(message);

        verifyNoInteractions(gameRoomService);
    }

    @Test
    public void handle_ignoresMessageWithNullPlayerId() {
        PlayerLeaveMessage message = new PlayerLeaveMessage(
                "room-123",
                null,
                PlayerLeaveMessage.LeaveReason.VOLUNTARY);

        listener.handle(message);

        verifyNoInteractions(gameRoomService);
    }

    @Test
    public void handle_doesNotRethrowExceptions() {
        PlayerLeaveMessage message = new PlayerLeaveMessage(
                "room-error",
                "player-error",
                PlayerLeaveMessage.LeaveReason.VOLUNTARY);

        doThrow(new RuntimeException("Test exception"))
                .when(gameRoomService).removePlayerFromRoom("room-error", "player-error");

        // Should not throw - exceptions are caught and logged
        listener.handle(message);

        verify(gameRoomService).removePlayerFromRoom("room-error", "player-error");
    }
}
