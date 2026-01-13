package com.online_games_service.statistical.controller;

import com.online_games_service.statistical.dto.PlayerAllStatisticsDto;
import com.online_games_service.statistical.dto.PlayerStatisticsDto;
import com.online_games_service.statistical.dto.RankingsDto;
import com.online_games_service.statistical.service.StatisticsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Comprehensive unit tests for StatisticsController.
 */
public class StatisticsControllerTest {

    private StatisticsController controller;
    private StatisticsService statisticsService;
    private HttpServletRequest request;

    @BeforeMethod
    public void setUp() {
        statisticsService = mock(StatisticsService.class);
        controller = new StatisticsController(statisticsService);
        request = mock(HttpServletRequest.class);
    }

    // ============================================================
    // GET MY STATISTICS TESTS
    // ============================================================

    @Test
    public void getMyStatistics_returnsStatsForAuthenticatedUser() {
        when(request.getAttribute("userId")).thenReturn("user123");
        
        PlayerStatisticsDto stats = new PlayerStatisticsDto("user123", "Alice", "MAKAO", 20, 15, 75.0);
        when(statisticsService.getPlayerStatistics("user123", "MAKAO")).thenReturn(stats);

        ResponseEntity<PlayerStatisticsDto> response = controller.getMyStatistics(request, "makao");

        assertEquals(response.getStatusCode().value(), 200);
        assertNotNull(response.getBody());
        assertEquals(response.getBody().getPlayerId(), "user123");
        assertEquals(response.getBody().getGamesPlayed(), 20);
    }

    @Test
    public void getMyStatistics_convertsGameTypeToUpperCase() {
        when(request.getAttribute("userId")).thenReturn("user123");
        when(statisticsService.getPlayerStatistics("user123", "MAKAO"))
                .thenReturn(new PlayerStatisticsDto("user123", "Alice", "MAKAO", 20, 15, 75.0));

        controller.getMyStatistics(request, "makao");

        verify(statisticsService).getPlayerStatistics("user123", "MAKAO");
    }

    @Test
    public void getMyStatistics_returns401WhenNoUserId() {
        when(request.getAttribute("userId")).thenReturn(null);

        ResponseEntity<PlayerStatisticsDto> response = controller.getMyStatistics(request, "makao");

        assertEquals(response.getStatusCode().value(), 401);
        assertNull(response.getBody());
    }

    // ============================================================
    // GET ALL MY STATISTICS TESTS
    // ============================================================

    @Test
    public void getAllMyStatistics_returnsAllStatsForAuthenticatedUser() {
        when(request.getAttribute("userId")).thenReturn("user123");
        
        List<PlayerStatisticsDto> statsList = Arrays.asList(
                new PlayerStatisticsDto("user123", "Alice", "MAKAO", 20, 15, 75.0),
                new PlayerStatisticsDto("user123", "Alice", "LUDO", 10, 3, 30.0)
        );
        PlayerAllStatisticsDto allStats = new PlayerAllStatisticsDto("user123", statsList);
        when(statisticsService.getAllPlayerStatistics("user123")).thenReturn(allStats);

        ResponseEntity<PlayerAllStatisticsDto> response = controller.getAllMyStatistics(request);

        assertEquals(response.getStatusCode().value(), 200);
        assertNotNull(response.getBody());
        assertEquals(response.getBody().getStatistics().size(), 2);
    }

    @Test
    public void getAllMyStatistics_returns401WhenNoUserId() {
        when(request.getAttribute("userId")).thenReturn(null);

        ResponseEntity<PlayerAllStatisticsDto> response = controller.getAllMyStatistics(request);

        assertEquals(response.getStatusCode().value(), 401);
        assertNull(response.getBody());
    }

    // ============================================================
    // GET PLAYER STATISTICS TESTS
    // ============================================================

    @Test
    public void getPlayerStatistics_returnsStatsForPlayer() {
        PlayerStatisticsDto stats = new PlayerStatisticsDto("player1", "Alice", "MAKAO", 20, 15, 75.0);
        when(statisticsService.getPlayerStatistics("player1", "MAKAO")).thenReturn(stats);

        ResponseEntity<PlayerStatisticsDto> response = controller.getPlayerStatistics("player1", "makao");

        assertEquals(response.getStatusCode().value(), 200);
        assertNotNull(response.getBody());
        assertEquals(response.getBody().getPlayerId(), "player1");
    }

    @Test
    public void getPlayerStatistics_convertsGameTypeToUpperCase() {
        when(statisticsService.getPlayerStatistics("player1", "LUDO"))
                .thenReturn(new PlayerStatisticsDto("player1", "Alice", "LUDO", 10, 5, 50.0));

        controller.getPlayerStatistics("player1", "ludo");

        verify(statisticsService).getPlayerStatistics("player1", "LUDO");
    }

    // ============================================================
    // GET ALL PLAYER STATISTICS TESTS
    // ============================================================

    @Test
    public void getAllPlayerStatistics_returnsAllStatsForPlayer() {
        List<PlayerStatisticsDto> statsList = Arrays.asList(
                new PlayerStatisticsDto("player1", "Alice", "MAKAO", 20, 15, 75.0)
        );
        PlayerAllStatisticsDto allStats = new PlayerAllStatisticsDto("player1", statsList);
        when(statisticsService.getAllPlayerStatistics("player1")).thenReturn(allStats);

        ResponseEntity<PlayerAllStatisticsDto> response = controller.getAllPlayerStatistics("player1");

        assertEquals(response.getStatusCode().value(), 200);
        assertNotNull(response.getBody());
        assertEquals(response.getBody().getPlayerId(), "player1");
    }

    // ============================================================
    // GET RANKINGS TESTS
    // ============================================================

    @Test
    public void getRankings_returnsRankingsForGameType() {
        List<PlayerStatisticsDto> topByPlayed = Arrays.asList(
                new PlayerStatisticsDto("player1", "Alice", "MAKAO", 100, 50, 50.0)
        );
        List<PlayerStatisticsDto> topByWon = Arrays.asList(
                new PlayerStatisticsDto("player1", "Alice", "MAKAO", 100, 50, 50.0)
        );
        RankingsDto rankings = new RankingsDto("MAKAO", topByPlayed, topByWon);
        when(statisticsService.getRankings("MAKAO", 30)).thenReturn(rankings);

        ResponseEntity<RankingsDto> response = controller.getRankings("makao", 30);

        assertEquals(response.getStatusCode().value(), 200);
        assertNotNull(response.getBody());
        assertEquals(response.getBody().getGameType(), "MAKAO");
    }

    @Test
    public void getRankings_convertsGameTypeToUpperCase() {
        when(statisticsService.getRankings("MAKAO", 30))
                .thenReturn(new RankingsDto("MAKAO", Collections.emptyList(), Collections.emptyList()));

        controller.getRankings("makao", 30);

        verify(statisticsService).getRankings("MAKAO", 30);
    }

    @Test
    public void getRankings_usesDefaultLimit() {
        when(statisticsService.getRankings("MAKAO", 30))
                .thenReturn(new RankingsDto("MAKAO", Collections.emptyList(), Collections.emptyList()));

        controller.getRankings("makao", 30);

        verify(statisticsService).getRankings("MAKAO", 30);
    }

    @Test
    public void getRankings_enforcesMinimumLimit() {
        when(statisticsService.getRankings("MAKAO", 1))
                .thenReturn(new RankingsDto("MAKAO", Collections.emptyList(), Collections.emptyList()));

        controller.getRankings("makao", 0);

        verify(statisticsService).getRankings("MAKAO", 1);
    }

    @Test
    public void getRankings_enforcesMaximumLimit() {
        when(statisticsService.getRankings("MAKAO", 100))
                .thenReturn(new RankingsDto("MAKAO", Collections.emptyList(), Collections.emptyList()));

        controller.getRankings("makao", 200);

        verify(statisticsService).getRankings("MAKAO", 100);
    }

    @Test
    public void getRankings_handlesNegativeLimit() {
        when(statisticsService.getRankings("MAKAO", 1))
                .thenReturn(new RankingsDto("MAKAO", Collections.emptyList(), Collections.emptyList()));

        controller.getRankings("makao", -5);

        verify(statisticsService).getRankings("MAKAO", 1);
    }

    // ============================================================
    // HEALTH CHECK TESTS
    // ============================================================

    @Test
    public void health_returnsOk() {
        ResponseEntity<String> response = controller.health();

        assertEquals(response.getStatusCode().value(), 200);
        assertEquals(response.getBody(), "OK");
    }
}
