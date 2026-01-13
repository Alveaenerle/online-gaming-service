package com.online_games_service.statistical.dto;

import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.testng.Assert.*;

/**
 * Unit tests for DTO classes.
 */
public class DtoTest {

    // ============================================================
    // PLAYER STATISTICS DTO TESTS
    // ============================================================

    @Test
    public void playerStatisticsDto_defaultConstructor() {
        PlayerStatisticsDto dto = new PlayerStatisticsDto();
        
        assertNull(dto.getPlayerId());
        assertNull(dto.getUsername());
        assertNull(dto.getGameType());
        assertEquals(dto.getGamesPlayed(), 0);
        assertEquals(dto.getGamesWon(), 0);
        assertEquals(dto.getWinRatio(), 0.0);
    }

    @Test
    public void playerStatisticsDto_allArgsConstructor() {
        PlayerStatisticsDto dto = new PlayerStatisticsDto("player1", "Alice", "MAKAO", 20, 15, 75.0);
        
        assertEquals(dto.getPlayerId(), "player1");
        assertEquals(dto.getUsername(), "Alice");
        assertEquals(dto.getGameType(), "MAKAO");
        assertEquals(dto.getGamesPlayed(), 20);
        assertEquals(dto.getGamesWon(), 15);
        assertEquals(dto.getWinRatio(), 75.0);
    }

    @Test
    public void playerStatisticsDto_setters() {
        PlayerStatisticsDto dto = new PlayerStatisticsDto();
        
        dto.setPlayerId("player1");
        dto.setUsername("Alice");
        dto.setGameType("LUDO");
        dto.setGamesPlayed(50);
        dto.setGamesWon(25);
        dto.setWinRatio(50.0);
        
        assertEquals(dto.getPlayerId(), "player1");
        assertEquals(dto.getUsername(), "Alice");
        assertEquals(dto.getGameType(), "LUDO");
        assertEquals(dto.getGamesPlayed(), 50);
        assertEquals(dto.getGamesWon(), 25);
        assertEquals(dto.getWinRatio(), 50.0);
    }

    @Test
    public void playerStatisticsDto_equality() {
        PlayerStatisticsDto dto1 = new PlayerStatisticsDto("player1", "Alice", "MAKAO", 20, 15, 75.0);
        PlayerStatisticsDto dto2 = new PlayerStatisticsDto("player1", "Alice", "MAKAO", 20, 15, 75.0);
        
        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    public void playerStatisticsDto_toString() {
        PlayerStatisticsDto dto = new PlayerStatisticsDto("player1", "Alice", "MAKAO", 20, 15, 75.0);
        
        String str = dto.toString();
        assertTrue(str.contains("player1"));
        assertTrue(str.contains("Alice"));
        assertTrue(str.contains("MAKAO"));
    }

    // ============================================================
    // RANKINGS DTO TESTS
    // ============================================================

    @Test
    public void rankingsDto_defaultConstructor() {
        RankingsDto dto = new RankingsDto();
        
        assertNull(dto.getGameType());
        assertNull(dto.getTopByGamesPlayed());
        assertNull(dto.getTopByGamesWon());
    }

    @Test
    public void rankingsDto_allArgsConstructor() {
        List<PlayerStatisticsDto> topByPlayed = Arrays.asList(
                new PlayerStatisticsDto("player1", "Alice", "MAKAO", 100, 50, 50.0)
        );
        List<PlayerStatisticsDto> topByWon = Arrays.asList(
                new PlayerStatisticsDto("player2", "Bob", "MAKAO", 80, 60, 75.0)
        );
        
        RankingsDto dto = new RankingsDto("MAKAO", topByPlayed, topByWon);
        
        assertEquals(dto.getGameType(), "MAKAO");
        assertEquals(dto.getTopByGamesPlayed().size(), 1);
        assertEquals(dto.getTopByGamesWon().size(), 1);
    }

    @Test
    public void rankingsDto_setters() {
        RankingsDto dto = new RankingsDto();
        
        dto.setGameType("LUDO");
        dto.setTopByGamesPlayed(Collections.emptyList());
        dto.setTopByGamesWon(Collections.emptyList());
        
        assertEquals(dto.getGameType(), "LUDO");
        assertTrue(dto.getTopByGamesPlayed().isEmpty());
        assertTrue(dto.getTopByGamesWon().isEmpty());
    }

    @Test
    public void rankingsDto_equality() {
        RankingsDto dto1 = new RankingsDto("MAKAO", Collections.emptyList(), Collections.emptyList());
        RankingsDto dto2 = new RankingsDto("MAKAO", Collections.emptyList(), Collections.emptyList());
        
        assertEquals(dto1, dto2);
    }

    // ============================================================
    // PLAYER ALL STATISTICS DTO TESTS
    // ============================================================

    @Test
    public void playerAllStatisticsDto_defaultConstructor() {
        PlayerAllStatisticsDto dto = new PlayerAllStatisticsDto();
        
        assertNull(dto.getPlayerId());
        assertNull(dto.getStatistics());
    }

    @Test
    public void playerAllStatisticsDto_allArgsConstructor() {
        List<PlayerStatisticsDto> stats = Arrays.asList(
                new PlayerStatisticsDto("player1", "Alice", "MAKAO", 20, 15, 75.0),
                new PlayerStatisticsDto("player1", "Alice", "LUDO", 10, 5, 50.0)
        );
        
        PlayerAllStatisticsDto dto = new PlayerAllStatisticsDto("player1", stats);
        
        assertEquals(dto.getPlayerId(), "player1");
        assertEquals(dto.getStatistics().size(), 2);
    }

    @Test
    public void playerAllStatisticsDto_setters() {
        PlayerAllStatisticsDto dto = new PlayerAllStatisticsDto();
        
        dto.setPlayerId("player1");
        dto.setStatistics(Collections.emptyList());
        
        assertEquals(dto.getPlayerId(), "player1");
        assertTrue(dto.getStatistics().isEmpty());
    }

    @Test
    public void playerAllStatisticsDto_equality() {
        PlayerAllStatisticsDto dto1 = new PlayerAllStatisticsDto("player1", Collections.emptyList());
        PlayerAllStatisticsDto dto2 = new PlayerAllStatisticsDto("player1", Collections.emptyList());
        
        assertEquals(dto1, dto2);
    }
}
