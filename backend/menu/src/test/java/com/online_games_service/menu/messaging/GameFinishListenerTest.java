package com.online_games_service.menu.messaging;

import com.online_games_service.common.enums.RoomStatus;
import com.online_games_service.common.messaging.GameFinishMessage;
import com.online_games_service.menu.service.GameRoomService;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.doThrow;
import static org.testng.Assert.expectThrows;

public class GameFinishListenerTest {

    @Mock
    private GameRoomService gameRoomService;

    private GameFinishListener listener;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        listener = new GameFinishListener(gameRoomService);
    }

    @Test
    public void handle_shouldCallService_whenMessageIsValid() {
        String roomId = "room-1";
        RoomStatus status = RoomStatus.FINISHED;
        GameFinishMessage message = new GameFinishMessage(roomId, status);

        listener.handle(message);

        verify(gameRoomService).markFinished(roomId, status);
    }

    @Test
    public void handle_shouldDoNothing_whenMessageIsNull() {
        listener.handle(null);
        verifyNoInteractions(gameRoomService);
    }

    @Test
    public void handle_shouldDoNothing_whenRoomIdIsNull() {
        GameFinishMessage message = new GameFinishMessage(null, RoomStatus.FINISHED);
        listener.handle(message);
        verifyNoInteractions(gameRoomService);
    }

    @Test
    public void handle_shouldDoNothing_whenRoomIdIsBlank() {
        GameFinishMessage message = new GameFinishMessage("  ", RoomStatus.FINISHED);
        listener.handle(message);
        verifyNoInteractions(gameRoomService);
    }

    @Test
    public void handle_shouldRethrow_whenServiceThrows() {
        String roomId = "room-1";
        RoomStatus status = RoomStatus.FINISHED;
        GameFinishMessage message = new GameFinishMessage(roomId, status);

        doThrow(new RuntimeException("Service error")).when(gameRoomService).markFinished(roomId, status);

        expectThrows(RuntimeException.class, () -> listener.handle(message));
    }
}
