package com.online_games_service.statistical.service;

import com.online_games_service.statistical.dto.PlayerAllStatisticsDto;
import com.online_games_service.statistical.dto.PlayerStatisticsDto;
import com.online_games_service.statistical.dto.RankingsDto;
import com.online_games_service.statistical.model.PlayerStatistics;
import com.online_games_service.statistical.repository.PlayerStatisticsRepository;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Comprehensive unit tests for StatisticsService.
 * Uses mocks for repository dependency.
 */
public class StatisticsServiceTest {

    private StatisticsService statisticsService;
    private PlayerStatisticsRepository statisticsRepository;

    @BeforeMethod
    public void setUp() {
        statisticsRepository = mock(PlayerStatisticsRepository.class);
        statisticsService = new StatisticsService(statisticsRepository);
    }

    // ============================================================
    // RECORD GAME RESULT - HAPPY PATH TESTS
    // ============================================================

    @Test
    public void recordGameResult_createsNewStatsForNewPlayer() {
        String gameType = "MAKAO";
        Map<String, String> participants = new HashMap<>();
        participants.put("player1", "Alice");
        String winnerId = "player1";

        when(statisticsRepository.findByPlayerIdAndGameType("player1", gameType))
                .thenReturn(Optional.empty());

        statisticsService.recordGameResult(gameType, participants, winnerId);

        ArgumentCaptor<PlayerStatistics> captor = ArgumentCaptor.forClass(PlayerStatistics.class);
        verify(statisticsRepository).save(captor.capture());

        PlayerStatistics saved = captor.getValue();
        assertEquals(saved.getPlayerId(), "player1");
        assertEquals(saved.getUsername(), "Alice");
        assertEquals(saved.getGameType(), "MAKAO");
        assertEquals(saved.getGamesPlayed(), 1);
        assertEquals(saved.getGamesWon(), 1);
    }

    @Test
    public void recordGameResult_updatesExistingStats() {
        String gameType = "MAKAO";
        Map<String, String> participants = new HashMap<>();
        participants.put("player1", "Alice");
        String winnerId = "player1";

        PlayerStatistics existing = new PlayerStatistics("id1", "player1", "Alice", "MAKAO", 10, 5);
        when(statisticsRepository.findByPlayerIdAndGameType("player1", gameType))
                .thenReturn(Optional.of(existing));

        statisticsService.recordGameResult(gameType, participants, winnerId);

        ArgumentCaptor<PlayerStatistics> captor = ArgumentCaptor.forClass(PlayerStatistics.class);
        verify(statisticsRepository).save(captor.capture());

        PlayerStatistics saved = captor.getValue();
        assertEquals(saved.getGamesPlayed(), 11);
        assertEquals(saved.getGamesWon(), 6);
    }

    @Test
    public void recordGameResult_incrementsPlayedButNotWonForLoser() {
        String gameType = "MAKAO";
        Map<String, String> participants = new HashMap<>();
        participants.put("player1", "Alice");
        participants.put("player2", "Bob");
        String winnerId = "player1";

        when(statisticsRepository.findByPlayerIdAndGameType("player1", gameType))
                .thenReturn(Optional.empty());
        when(statisticsRepository.findByPlayerIdAndGameType("player2", gameType))
                .thenReturn(Optional.empty());

        statisticsService.recordGameResult(gameType, participants, winnerId);

        ArgumentCaptor<PlayerStatistics> captor = ArgumentCaptor.forClass(PlayerStatistics.class);
        verify(statisticsRepository, times(2)).save(captor.capture());

        List<PlayerStatistics> savedList = captor.getAllValues();
        
        // Find player2's stats
        PlayerStatistics player2Stats = savedList.stream()
                .filter(s -> "player2".equals(s.getPlayerId()))
                .findFirst()
                .orElse(null);

        assertNotNull(player2Stats);
        assertEquals(player2Stats.getGamesPlayed(), 1);
        assertEquals(player2Stats.getGamesWon(), 0);
    }

    @Test
    public void recordGameResult_handlesMultiplePlayers() {
        String gameType = "MAKAO";
        Map<String, String> participants = new HashMap<>();
        participants.put("player1", "Alice");
        participants.put("player2", "Bob");
        participants.put("player3", "Charlie");
        String winnerId = "player2";

        when(statisticsRepository.findByPlayerIdAndGameType(anyString(), eq(gameType)))
                .thenReturn(Optional.empty());

        statisticsService.recordGameResult(gameType, participants, winnerId);

        verify(statisticsRepository, times(3)).save(any(PlayerStatistics.class));
    }

    @Test
    public void recordGameResult_updatesUsernameIfChanged() {
        String gameType = "MAKAO";
        Map<String, String> participants = new HashMap<>();
        participants.put("player1", "NewAlice");
        String winnerId = null;

        PlayerStatistics existing = new PlayerStatistics("id1", "player1", "OldAlice", "MAKAO", 10, 5);
        when(statisticsRepository.findByPlayerIdAndGameType("player1", gameType))
                .thenReturn(Optional.of(existing));

        statisticsService.recordGameResult(gameType, participants, winnerId);

        ArgumentCaptor<PlayerStatistics> captor = ArgumentCaptor.forClass(PlayerStatistics.class);
        verify(statisticsRepository).save(captor.capture());

        assertEquals(captor.getValue().getUsername(), "NewAlice");
    }

    // ============================================================
    // RECORD GAME RESULT - BOT FILTERING TESTS
    // ============================================================

    @Test
    public void recordGameResult_skipsBotPlayers() {
        String gameType = "MAKAO";
        Map<String, String> participants = new HashMap<>();
        participants.put("player1", "Alice");
        participants.put("bot_easy_123", "EasyBot");
        String winnerId = "player1";

        when(statisticsRepository.findByPlayerIdAndGameType("player1", gameType))
                .thenReturn(Optional.empty());

        statisticsService.recordGameResult(gameType, participants, winnerId);

        // Only player1 should be saved, not the bot
        verify(statisticsRepository, times(1)).save(any(PlayerStatistics.class));

        ArgumentCaptor<PlayerStatistics> captor = ArgumentCaptor.forClass(PlayerStatistics.class);
        verify(statisticsRepository).save(captor.capture());
        assertEquals(captor.getValue().getPlayerId(), "player1");
    }

    @Test
    public void recordGameResult_skipsBotWithUppercase() {
        String gameType = "MAKAO";
        Map<String, String> participants = new HashMap<>();
        participants.put("BOT_HARD", "HardBot");
        String winnerId = null;

        statisticsService.recordGameResult(gameType, participants, winnerId);

        verify(statisticsRepository, never()).save(any(PlayerStatistics.class));
    }

    @Test
    public void recordGameResult_skipsBotContainingBotInId() {
        String gameType = "MAKAO";
        Map<String, String> participants = new HashMap<>();
        participants.put("some-bot-id", "Bot");
        String winnerId = null;

        statisticsService.recordGameResult(gameType, participants, winnerId);

        verify(statisticsRepository, never()).save(any(PlayerStatistics.class));
    }

    @Test
    public void isBot_returnsTrueForBotIds() {
        assertTrue(statisticsService.isBot("bot_123"));
        assertTrue(statisticsService.isBot("BOT_EASY"));
        assertTrue(statisticsService.isBot("some-bot-player"));
        assertTrue(statisticsService.isBot("botPlayer"));
    }

    @Test
    public void isBot_returnsFalseForNormalIds() {
        assertFalse(statisticsService.isBot("player123"));
        assertFalse(statisticsService.isBot("abc-def-123"));
        assertFalse(statisticsService.isBot("robert")); // Contains "bot" but not as standalone
    }

    @Test
    public void isBot_returnsFalseForNull() {
        assertFalse(statisticsService.isBot(null));
    }

    // ============================================================
    // RECORD GAME RESULT - NULL/EMPTY INPUT TESTS
    // ============================================================

    @Test
    public void recordGameResult_handlesNullGameType() {
        Map<String, String> participants = new HashMap<>();
        participants.put("player1", "Alice");

        statisticsService.recordGameResult(null, participants, "player1");

        verify(statisticsRepository, never()).save(any(PlayerStatistics.class));
    }

    @Test
    public void recordGameResult_handlesBlankGameType() {
        Map<String, String> participants = new HashMap<>();
        participants.put("player1", "Alice");

        statisticsService.recordGameResult("   ", participants, "player1");

        verify(statisticsRepository, never()).save(any(PlayerStatistics.class));
    }

    @Test
    public void recordGameResult_handlesNullParticipants() {
        statisticsService.recordGameResult("MAKAO", null, "player1");

        verify(statisticsRepository, never()).save(any(PlayerStatistics.class));
    }

    @Test
    public void recordGameResult_handlesEmptyParticipants() {
        statisticsService.recordGameResult("MAKAO", new HashMap<>(), "player1");

        verify(statisticsRepository, never()).save(any(PlayerStatistics.class));
    }

    @Test
    public void recordGameResult_handlesNullPlayerId() {
        Map<String, String> participants = new HashMap<>();
        participants.put(null, "Alice");
        participants.put("player1", "Bob");

        when(statisticsRepository.findByPlayerIdAndGameType("player1", "MAKAO"))
                .thenReturn(Optional.empty());

        statisticsService.recordGameResult("MAKAO", participants, "player1");

        // Only player1 should be processed
        verify(statisticsRepository, times(1)).save(any(PlayerStatistics.class));
    }

    @Test
    public void recordGameResult_handlesBlankPlayerId() {
        Map<String, String> participants = new HashMap<>();
        participants.put("   ", "Alice");
        participants.put("player1", "Bob");

        when(statisticsRepository.findByPlayerIdAndGameType("player1", "MAKAO"))
                .thenReturn(Optional.empty());

        statisticsService.recordGameResult("MAKAO", participants, "player1");

        verify(statisticsRepository, times(1)).save(any(PlayerStatistics.class));
    }

    @Test
    public void recordGameResult_handlesNullWinnerId() {
        Map<String, String> participants = new HashMap<>();
        participants.put("player1", "Alice");

        when(statisticsRepository.findByPlayerIdAndGameType("player1", "MAKAO"))
                .thenReturn(Optional.empty());

        statisticsService.recordGameResult("MAKAO", participants, null);

        ArgumentCaptor<PlayerStatistics> captor = ArgumentCaptor.forClass(PlayerStatistics.class);
        verify(statisticsRepository).save(captor.capture());

        assertEquals(captor.getValue().getGamesPlayed(), 1);
        assertEquals(captor.getValue().getGamesWon(), 0);
    }

    // ============================================================
    // GET PLAYER STATISTICS TESTS
    // ============================================================

    @Test
    public void getPlayerStatistics_returnsStatsWhenExists() {
        PlayerStatistics stats = new PlayerStatistics("id1", "player1", "Alice", "MAKAO", 20, 15);
        when(statisticsRepository.findByPlayerIdAndGameType("player1", "MAKAO"))
                .thenReturn(Optional.of(stats));

        PlayerStatisticsDto result = statisticsService.getPlayerStatistics("player1", "MAKAO");

        assertNotNull(result);
        assertEquals(result.getPlayerId(), "player1");
        assertEquals(result.getUsername(), "Alice");
        assertEquals(result.getGameType(), "MAKAO");
        assertEquals(result.getGamesPlayed(), 20);
        assertEquals(result.getGamesWon(), 15);
        assertEquals(result.getWinRatio(), 75.0);
    }

    @Test
    public void getPlayerStatistics_returnsEmptyStatsWhenNotExists() {
        when(statisticsRepository.findByPlayerIdAndGameType("player1", "MAKAO"))
                .thenReturn(Optional.empty());

        PlayerStatisticsDto result = statisticsService.getPlayerStatistics("player1", "MAKAO");

        assertNotNull(result);
        assertEquals(result.getPlayerId(), "player1");
        assertEquals(result.getGameType(), "MAKAO");
        assertEquals(result.getGamesPlayed(), 0);
        assertEquals(result.getGamesWon(), 0);
        assertEquals(result.getWinRatio(), 0.0);
    }

    @Test
    public void getPlayerStatistics_returnsNullForNullPlayerId() {
        PlayerStatisticsDto result = statisticsService.getPlayerStatistics(null, "MAKAO");
        assertNull(result);
    }

    @Test
    public void getPlayerStatistics_returnsNullForNullGameType() {
        PlayerStatisticsDto result = statisticsService.getPlayerStatistics("player1", null);
        assertNull(result);
    }

    // ============================================================
    // GET ALL PLAYER STATISTICS TESTS
    // ============================================================

    @Test
    public void getAllPlayerStatistics_returnsAllGameTypes() {
        List<PlayerStatistics> statsList = Arrays.asList(
                new PlayerStatistics("id1", "player1", "Alice", "MAKAO", 20, 15),
                new PlayerStatistics("id2", "player1", "Alice", "LUDO", 10, 3)
        );
        when(statisticsRepository.findByPlayerId("player1")).thenReturn(statsList);

        PlayerAllStatisticsDto result = statisticsService.getAllPlayerStatistics("player1");

        assertNotNull(result);
        assertEquals(result.getPlayerId(), "player1");
        assertEquals(result.getStatistics().size(), 2);
    }

    @Test
    public void getAllPlayerStatistics_returnsEmptyListWhenNoStats() {
        when(statisticsRepository.findByPlayerId("player1")).thenReturn(Collections.emptyList());

        PlayerAllStatisticsDto result = statisticsService.getAllPlayerStatistics("player1");

        assertNotNull(result);
        assertEquals(result.getPlayerId(), "player1");
        assertTrue(result.getStatistics().isEmpty());
    }

    @Test
    public void getAllPlayerStatistics_handlesNullPlayerId() {
        PlayerAllStatisticsDto result = statisticsService.getAllPlayerStatistics(null);

        assertNotNull(result);
        assertNull(result.getPlayerId());
        assertTrue(result.getStatistics().isEmpty());
    }

    // ============================================================
    // GET RANKINGS TESTS
    // ============================================================

    @Test
    public void getRankings_returnsTopPlayersForGameType() {
        List<PlayerStatistics> topByPlayed = Arrays.asList(
                new PlayerStatistics("id1", "player1", "Alice", "MAKAO", 100, 50),
                new PlayerStatistics("id2", "player2", "Bob", "MAKAO", 80, 40)
        );
        List<PlayerStatistics> topByWon = Arrays.asList(
                new PlayerStatistics("id1", "player1", "Alice", "MAKAO", 100, 50),
                new PlayerStatistics("id3", "player3", "Charlie", "MAKAO", 60, 45)
        );

        when(statisticsRepository.findByGameTypeOrderByGamesPlayedDesc(eq("MAKAO"), any(Pageable.class)))
                .thenReturn(topByPlayed);
        when(statisticsRepository.findByGameTypeOrderByGamesWonDesc(eq("MAKAO"), any(Pageable.class)))
                .thenReturn(topByWon);

        RankingsDto result = statisticsService.getRankings("MAKAO");

        assertNotNull(result);
        assertEquals(result.getGameType(), "MAKAO");
        assertEquals(result.getTopByGamesPlayed().size(), 2);
        assertEquals(result.getTopByGamesWon().size(), 2);
    }

    @Test
    public void getRankings_usesDefaultLimitOf30() {
        when(statisticsRepository.findByGameTypeOrderByGamesPlayedDesc(eq("MAKAO"), any(Pageable.class)))
                .thenReturn(Collections.emptyList());
        when(statisticsRepository.findByGameTypeOrderByGamesWonDesc(eq("MAKAO"), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        statisticsService.getRankings("MAKAO");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(statisticsRepository).findByGameTypeOrderByGamesPlayedDesc(eq("MAKAO"), captor.capture());
        
        assertEquals(captor.getValue().getPageSize(), 30);
    }

    @Test
    public void getRankings_respectsCustomLimit() {
        when(statisticsRepository.findByGameTypeOrderByGamesPlayedDesc(eq("MAKAO"), any(Pageable.class)))
                .thenReturn(Collections.emptyList());
        when(statisticsRepository.findByGameTypeOrderByGamesWonDesc(eq("MAKAO"), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        statisticsService.getRankings("MAKAO", 10);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(statisticsRepository).findByGameTypeOrderByGamesPlayedDesc(eq("MAKAO"), captor.capture());
        
        assertEquals(captor.getValue().getPageSize(), 10);
    }

    @Test
    public void getRankings_returnsEmptyForNullGameType() {
        RankingsDto result = statisticsService.getRankings(null);

        assertNotNull(result);
        assertNull(result.getGameType());
        assertTrue(result.getTopByGamesPlayed().isEmpty());
        assertTrue(result.getTopByGamesWon().isEmpty());
    }

    @Test
    public void getRankings_returnsEmptyForBlankGameType() {
        RankingsDto result = statisticsService.getRankings("   ");

        assertNotNull(result);
        assertTrue(result.getTopByGamesPlayed().isEmpty());
        assertTrue(result.getTopByGamesWon().isEmpty());
    }

    @Test
    public void getRankings_returnsEmptyListsWhenNoData() {
        when(statisticsRepository.findByGameTypeOrderByGamesPlayedDesc(eq("MAKAO"), any(Pageable.class)))
                .thenReturn(Collections.emptyList());
        when(statisticsRepository.findByGameTypeOrderByGamesWonDesc(eq("MAKAO"), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        RankingsDto result = statisticsService.getRankings("MAKAO");

        assertNotNull(result);
        assertTrue(result.getTopByGamesPlayed().isEmpty());
        assertTrue(result.getTopByGamesWon().isEmpty());
    }
}
