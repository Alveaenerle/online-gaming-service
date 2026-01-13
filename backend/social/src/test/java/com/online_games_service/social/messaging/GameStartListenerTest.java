package com.online_games_service.social.messaging;

import com.online_games_service.common.enums.GameType;
import com.online_games_service.common.messaging.GameStartMessage;
import com.online_games_service.social.service.GameInviteService;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

/**
 * Unit tests for GameStartListener.
 */
public class GameStartListenerTest {

    private GameStartListener listener;
    private GameInviteService gameInviteService;

    @BeforeMethod
    public void setUp() {
        gameInviteService = mock(GameInviteService.class);
        listener = new GameStartListener(gameInviteService);
    }

    @Test
    public void handleGameStart_ValidMessage_DeletesInvites() {
        // Given
        Map<String, String> players = new HashMap<>();
        players.put("user1", "Alice");
        players.put("user2", "Bob");

        Map<String, String> avatars = new HashMap<>();
        avatars.put("user1", "avatar1.png");
        avatars.put("user2", "avatar2.png");

        GameStartMessage message = new GameStartMessage(
                "room123",
                "Fun Game",
                GameType.MAKAO,
                players,
                avatars,
                4,
                "user1",
                "Alice"
        );

        // When
        listener.handleGameStart(message);

        // Then
        verify(gameInviteService).deleteInvitesForLobby("room123");
    }

    @Test
    public void handleGameStart_NullMessage_DoesNothing() {
        // When
        listener.handleGameStart(null);

        // Then
        verify(gameInviteService, never()).deleteInvitesForLobby(any());
    }

    @Test
    public void handleGameStart_NullRoomId_DoesNothing() {
        // Given
        GameStartMessage message = new GameStartMessage(
                null,
                "Room",
                GameType.LUDO,
                new HashMap<>(),
                new HashMap<>(),
                2,
                "host",
                "Host"
        );

        // When
        listener.handleGameStart(message);

        // Then
        verify(gameInviteService, never()).deleteInvitesForLobby(any());
    }

    @Test
    public void handleGameStart_BlankRoomId_DoesNothing() {
        // Given
        GameStartMessage message = new GameStartMessage(
                "   ",
                "Room",
                GameType.MAKAO,
                new HashMap<>(),
                new HashMap<>(),
                4,
                "host",
                "Host"
        );

        // When
        listener.handleGameStart(message);

        // Then
        verify(gameInviteService, never()).deleteInvitesForLobby(any());
    }

    @Test
    public void handleGameStart_ServiceThrowsException_DoesNotPropagate() {
        // Given
        Map<String, String> players = new HashMap<>();
        players.put("user1", "Player");

        Map<String, String> avatars = new HashMap<>();
        avatars.put("user1", "avatar.png");

        GameStartMessage message = new GameStartMessage(
                "room456",
                "Game Room",
                GameType.LUDO,
                players,
                avatars,
                2,
                "user1",
                "Player"
        );

        when(gameInviteService.deleteInvitesForLobby("room456"))
                .thenThrow(new RuntimeException("Redis error"));

        // When - should not throw
        listener.handleGameStart(message);

        // Then
        verify(gameInviteService).deleteInvitesForLobby("room456");
    }

    @Test
    public void handleGameStart_DifferentGameTypes_ProcessesBoth() {
        // Given
        GameStartMessage makaoMessage = new GameStartMessage(
                "makao-room",
                "Makao Game",
                GameType.MAKAO,
                Map.of("u1", "p1"),
                Map.of("u1", "avatar1.png"),
                4,
                "u1",
                "p1"
        );

        GameStartMessage ludoMessage = new GameStartMessage(
                "ludo-room",
                "Ludo Game",
                GameType.LUDO,
                Map.of("u2", "p2"),
                Map.of("u2", "avatar2.png"),
                4,
                "u2",
                "p2"
        );

        // When
        listener.handleGameStart(makaoMessage);
        listener.handleGameStart(ludoMessage);

        // Then
        verify(gameInviteService).deleteInvitesForLobby("makao-room");
        verify(gameInviteService).deleteInvitesForLobby("ludo-room");
    }
}
