package com.online_games_service.statistical.messaging;

import com.online_games_service.common.messaging.GameResultMessage;
import com.online_games_service.statistical.service.StatisticsService;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Comprehensive unit tests for GameResultListener.
 */
public class GameResultListenerTest {

    private GameResultListener listener;
    private StatisticsService statisticsService;

    @BeforeMethod
    public void setUp() {
        statisticsService = mock(StatisticsService.class);
        listener = new GameResultListener(statisticsService);
    }

    // ============================================================
    // HAPPY PATH TESTS
    // ============================================================

    @Test
    public void handleGameResult_processesValidMessage() {
        Map<String, String> participants = new HashMap<>();
        participants.put("player1", "Alice");
        participants.put("player2", "Bob");

        Map<String, Integer> placements = new HashMap<>();
        placements.put("player1", 1);
        placements.put("player2", 2);

        GameResultMessage message = new GameResultMessage(
                "room123",
                "MAKAO",
                participants,
                placements,
                "player1"
        );

        listener.handleGameResult(message);

        verify(statisticsService).recordGameResult("MAKAO", participants, "player1");
    }

    @Test
    public void handleGameResult_passesCorrectGameType() {
        Map<String, String> participants = new HashMap<>();
        participants.put("player1", "Alice");

        GameResultMessage message = new GameResultMessage(
                "room123",
                "LUDO",
                participants,
                new HashMap<>(),
                "player1"
        );

        listener.handleGameResult(message);

        ArgumentCaptor<String> gameTypeCaptor = ArgumentCaptor.forClass(String.class);
        verify(statisticsService).recordGameResult(gameTypeCaptor.capture(), any(), any());

        assertEquals(gameTypeCaptor.getValue(), "LUDO");
    }

    @Test
    public void handleGameResult_passesCorrectWinnerId() {
        Map<String, String> participants = new HashMap<>();
        participants.put("player1", "Alice");
        participants.put("player2", "Bob");

        GameResultMessage message = new GameResultMessage(
                "room123",
                "MAKAO",
                participants,
                new HashMap<>(),
                "player2"
        );

        listener.handleGameResult(message);

        ArgumentCaptor<String> winnerCaptor = ArgumentCaptor.forClass(String.class);
        verify(statisticsService).recordGameResult(any(), any(), winnerCaptor.capture());

        assertEquals(winnerCaptor.getValue(), "player2");
    }

    @Test
    public void handleGameResult_handlesNullWinnerId() {
        Map<String, String> participants = new HashMap<>();
        participants.put("player1", "Alice");

        GameResultMessage message = new GameResultMessage(
                "room123",
                "MAKAO",
                participants,
                new HashMap<>(),
                null
        );

        listener.handleGameResult(message);

        verify(statisticsService).recordGameResult("MAKAO", participants, null);
    }

    // ============================================================
    // NULL/EMPTY INPUT HANDLING TESTS
    // ============================================================

    @Test
    public void handleGameResult_skipsNullMessage() {
        listener.handleGameResult(null);

        verify(statisticsService, never()).recordGameResult(any(), any(), any());
    }

    @Test
    public void handleGameResult_skipsMessageWithNullRoomId() {
        Map<String, String> participants = new HashMap<>();
        participants.put("player1", "Alice");

        GameResultMessage message = new GameResultMessage(
                null,
                "MAKAO",
                participants,
                new HashMap<>(),
                "player1"
        );

        listener.handleGameResult(message);

        verify(statisticsService, never()).recordGameResult(any(), any(), any());
    }

    @Test
    public void handleGameResult_skipsMessageWithBlankRoomId() {
        Map<String, String> participants = new HashMap<>();
        participants.put("player1", "Alice");

        GameResultMessage message = new GameResultMessage(
                "   ",
                "MAKAO",
                participants,
                new HashMap<>(),
                "player1"
        );

        listener.handleGameResult(message);

        verify(statisticsService, never()).recordGameResult(any(), any(), any());
    }

    @Test
    public void handleGameResult_skipsMessageWithNullGameType() {
        Map<String, String> participants = new HashMap<>();
        participants.put("player1", "Alice");

        GameResultMessage message = new GameResultMessage(
                "room123",
                null,
                participants,
                new HashMap<>(),
                "player1"
        );

        listener.handleGameResult(message);

        verify(statisticsService, never()).recordGameResult(any(), any(), any());
    }

    @Test
    public void handleGameResult_skipsMessageWithBlankGameType() {
        Map<String, String> participants = new HashMap<>();
        participants.put("player1", "Alice");

        GameResultMessage message = new GameResultMessage(
                "room123",
                "   ",
                participants,
                new HashMap<>(),
                "player1"
        );

        listener.handleGameResult(message);

        verify(statisticsService, never()).recordGameResult(any(), any(), any());
    }

    @Test
    public void handleGameResult_skipsMessageWithNullParticipants() {
        GameResultMessage message = new GameResultMessage(
                "room123",
                "MAKAO",
                null,
                new HashMap<>(),
                "player1"
        );

        listener.handleGameResult(message);

        verify(statisticsService, never()).recordGameResult(any(), any(), any());
    }

    @Test
    public void handleGameResult_skipsMessageWithEmptyParticipants() {
        GameResultMessage message = new GameResultMessage(
                "room123",
                "MAKAO",
                new HashMap<>(),
                new HashMap<>(),
                "player1"
        );

        listener.handleGameResult(message);

        verify(statisticsService, never()).recordGameResult(any(), any(), any());
    }

    // ============================================================
    // ERROR HANDLING TESTS
    // ============================================================

    @Test
    public void handleGameResult_continuesAfterServiceException() {
        Map<String, String> participants = new HashMap<>();
        participants.put("player1", "Alice");

        GameResultMessage message = new GameResultMessage(
                "room123",
                "MAKAO",
                participants,
                new HashMap<>(),
                "player1"
        );

        doThrow(new RuntimeException("Database error"))
                .when(statisticsService).recordGameResult(any(), any(), any());

        // Should not throw, just log the error
        listener.handleGameResult(message);

        verify(statisticsService).recordGameResult("MAKAO", participants, "player1");
    }

    // ============================================================
    // MULTIPLE PARTICIPANTS TESTS
    // ============================================================

    @Test
    public void handleGameResult_handlesManyParticipants() {
        Map<String, String> participants = new HashMap<>();
        for (int i = 1; i <= 10; i++) {
            participants.put("player" + i, "User" + i);
        }

        GameResultMessage message = new GameResultMessage(
                "room123",
                "MAKAO",
                participants,
                new HashMap<>(),
                "player5"
        );

        listener.handleGameResult(message);

        ArgumentCaptor<Map<String, String>> participantsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(statisticsService).recordGameResult(eq("MAKAO"), participantsCaptor.capture(), eq("player5"));

        assertEquals(participantsCaptor.getValue().size(), 10);
    }
}
