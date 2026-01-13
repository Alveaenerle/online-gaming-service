package com.online_games_service.statistical.model;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Comprehensive unit tests for PlayerStatistics model.
 */
public class PlayerStatisticsTest {

    // ============================================================
    // CONSTRUCTOR TESTS
    // ============================================================

    @Test
    public void defaultConstructor_createsEmptyObject() {
        PlayerStatistics stats = new PlayerStatistics();
        
        assertNull(stats.getId());
        assertNull(stats.getPlayerId());
        assertNull(stats.getUsername());
        assertNull(stats.getGameType());
        assertEquals(stats.getGamesPlayed(), 0);
        assertEquals(stats.getGamesWon(), 0);
    }

    @Test
    public void allArgsConstructor_setsAllFields() {
        PlayerStatistics stats = new PlayerStatistics("id1", "player1", "testUser", "MAKAO", 10, 5);
        
        assertEquals(stats.getId(), "id1");
        assertEquals(stats.getPlayerId(), "player1");
        assertEquals(stats.getUsername(), "testUser");
        assertEquals(stats.getGameType(), "MAKAO");
        assertEquals(stats.getGamesPlayed(), 10);
        assertEquals(stats.getGamesWon(), 5);
    }

    @Test
    public void threeArgConstructor_initializesWithZeroStats() {
        PlayerStatistics stats = new PlayerStatistics("player1", "testUser", "MAKAO");
        
        assertEquals(stats.getPlayerId(), "player1");
        assertEquals(stats.getUsername(), "testUser");
        assertEquals(stats.getGameType(), "MAKAO");
        assertEquals(stats.getGamesPlayed(), 0);
        assertEquals(stats.getGamesWon(), 0);
    }

    // ============================================================
    // INCREMENT GAMES PLAYED TESTS
    // ============================================================

    @Test
    public void incrementGamesPlayed_increasesCountByOne() {
        PlayerStatistics stats = new PlayerStatistics("player1", "testUser", "MAKAO");
        
        stats.incrementGamesPlayed();
        assertEquals(stats.getGamesPlayed(), 1);
        
        stats.incrementGamesPlayed();
        assertEquals(stats.getGamesPlayed(), 2);
    }

    @Test
    public void incrementGamesPlayed_worksFromInitialZero() {
        PlayerStatistics stats = new PlayerStatistics();
        stats.setGamesPlayed(0);
        
        stats.incrementGamesPlayed();
        assertEquals(stats.getGamesPlayed(), 1);
    }

    @Test
    public void incrementGamesPlayed_worksFromNonZero() {
        PlayerStatistics stats = new PlayerStatistics("id", "player1", "user", "MAKAO", 50, 25);
        
        stats.incrementGamesPlayed();
        assertEquals(stats.getGamesPlayed(), 51);
    }

    // ============================================================
    // INCREMENT GAMES WON TESTS
    // ============================================================

    @Test
    public void incrementGamesWon_increasesCountByOne() {
        PlayerStatistics stats = new PlayerStatistics("player1", "testUser", "MAKAO");
        
        stats.incrementGamesWon();
        assertEquals(stats.getGamesWon(), 1);
        
        stats.incrementGamesWon();
        assertEquals(stats.getGamesWon(), 2);
    }

    @Test
    public void incrementGamesWon_worksFromInitialZero() {
        PlayerStatistics stats = new PlayerStatistics();
        stats.setGamesWon(0);
        
        stats.incrementGamesWon();
        assertEquals(stats.getGamesWon(), 1);
    }

    @Test
    public void incrementGamesWon_worksFromNonZero() {
        PlayerStatistics stats = new PlayerStatistics("id", "player1", "user", "MAKAO", 50, 25);
        
        stats.incrementGamesWon();
        assertEquals(stats.getGamesWon(), 26);
    }

    // ============================================================
    // WIN RATIO TESTS
    // ============================================================

    @Test
    public void getWinRatio_returnsZeroWhenNoGamesPlayed() {
        PlayerStatistics stats = new PlayerStatistics("player1", "testUser", "MAKAO");
        
        assertEquals(stats.getWinRatio(), 0.0);
    }

    @Test
    public void getWinRatio_returns100PercentWhenAllWon() {
        PlayerStatistics stats = new PlayerStatistics("id", "player1", "user", "MAKAO", 10, 10);
        
        assertEquals(stats.getWinRatio(), 100.0);
    }

    @Test
    public void getWinRatio_returns50PercentForHalfWon() {
        PlayerStatistics stats = new PlayerStatistics("id", "player1", "user", "MAKAO", 10, 5);
        
        assertEquals(stats.getWinRatio(), 50.0);
    }

    @Test
    public void getWinRatio_returns0WhenNoWins() {
        PlayerStatistics stats = new PlayerStatistics("id", "player1", "user", "MAKAO", 10, 0);
        
        assertEquals(stats.getWinRatio(), 0.0);
    }

    @Test
    public void getWinRatio_calculatesCorrectPercentageForArbitraryValues() {
        PlayerStatistics stats = new PlayerStatistics("id", "player1", "user", "MAKAO", 8, 3);
        
        double expected = (3.0 / 8.0) * 100.0;
        assertEquals(stats.getWinRatio(), expected, 0.001);
    }

    @Test
    public void getWinRatio_handlesLargeNumbers() {
        PlayerStatistics stats = new PlayerStatistics("id", "player1", "user", "MAKAO", 1000000, 333333);
        
        double expected = (333333.0 / 1000000.0) * 100.0;
        assertEquals(stats.getWinRatio(), expected, 0.001);
    }

    // ============================================================
    // SETTER TESTS
    // ============================================================

    @Test
    public void setters_updateFieldsCorrectly() {
        PlayerStatistics stats = new PlayerStatistics();
        
        stats.setId("newId");
        stats.setPlayerId("newPlayerId");
        stats.setUsername("newUsername");
        stats.setGameType("LUDO");
        stats.setGamesPlayed(100);
        stats.setGamesWon(75);
        
        assertEquals(stats.getId(), "newId");
        assertEquals(stats.getPlayerId(), "newPlayerId");
        assertEquals(stats.getUsername(), "newUsername");
        assertEquals(stats.getGameType(), "LUDO");
        assertEquals(stats.getGamesPlayed(), 100);
        assertEquals(stats.getGamesWon(), 75);
    }

    // ============================================================
    // EQUALS AND HASHCODE TESTS (from Lombok @Data)
    // ============================================================

    @Test
    public void equals_returnsTrueForSameObject() {
        PlayerStatistics stats = new PlayerStatistics("id", "player1", "user", "MAKAO", 10, 5);
        
        assertEquals(stats, stats);
    }

    @Test
    public void equals_returnsTrueForEqualObjects() {
        PlayerStatistics stats1 = new PlayerStatistics("id", "player1", "user", "MAKAO", 10, 5);
        PlayerStatistics stats2 = new PlayerStatistics("id", "player1", "user", "MAKAO", 10, 5);
        
        assertEquals(stats1, stats2);
    }

    @Test
    public void equals_returnsFalseForDifferentObjects() {
        PlayerStatistics stats1 = new PlayerStatistics("id1", "player1", "user", "MAKAO", 10, 5);
        PlayerStatistics stats2 = new PlayerStatistics("id2", "player2", "user", "MAKAO", 10, 5);
        
        assertNotEquals(stats1, stats2);
    }

    @Test
    public void hashCode_sameForEqualObjects() {
        PlayerStatistics stats1 = new PlayerStatistics("id", "player1", "user", "MAKAO", 10, 5);
        PlayerStatistics stats2 = new PlayerStatistics("id", "player1", "user", "MAKAO", 10, 5);
        
        assertEquals(stats1.hashCode(), stats2.hashCode());
    }

    // ============================================================
    // TOSTRING TEST (from Lombok @Data)
    // ============================================================

    @Test
    public void toString_containsRelevantInfo() {
        PlayerStatistics stats = new PlayerStatistics("id", "player1", "testUser", "MAKAO", 10, 5);
        
        String str = stats.toString();
        assertTrue(str.contains("player1"));
        assertTrue(str.contains("testUser"));
        assertTrue(str.contains("MAKAO"));
    }
}
