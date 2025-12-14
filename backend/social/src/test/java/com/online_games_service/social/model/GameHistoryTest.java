package com.online_games_service.social.model;

import com.online_games_service.common.enums.GameType;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.LocalDateTime;

public class GameHistoryTest {

    @Test
    public void shouldCreateHistoryWithCurrentDate() {
        // Given
        String accountId = "user_123";
        GameType gameType = GameType.LUDO;
        String matchId = "match_abc";
        boolean isWinner = true;

        // When
        GameHistory history = new GameHistory(accountId, gameType, matchId, isWinner);

        // Then
        Assert.assertEquals(history.getAccountId(), accountId);
        Assert.assertEquals(history.getGameType(), gameType);
        Assert.assertEquals(history.getMatchId(), matchId);
        Assert.assertTrue(history.isWinner());
        
        Assert.assertNotNull(history.getPlayedAt(), "PlayedAt date should be set automatically");
        
        Assert.assertNull(history.getId());
    }

    @Test
    public void testLombokEqualsAndHashCode() {
        GameHistory h1 = new GameHistory("userA", GameType.MAKAO, "m1", false);
        GameHistory h2 = new GameHistory("userA", GameType.MAKAO, "m1", false);

        LocalDateTime fixedTime = LocalDateTime.of(2025, 12, 14, 12, 0);
        h1.setPlayedAt(fixedTime);
        h2.setPlayedAt(fixedTime);

        // When & Then
        Assert.assertEquals(h1, h2, "Objects with same data should be equal");
        Assert.assertEquals(h1.hashCode(), h2.hashCode(), "HashCodes should be equal");
    }

    @Test
    public void testSettersAndGetters() {
        // Given
        GameHistory history = new GameHistory();

        // When
        history.setAccountId("user_test");
        history.setGameType(GameType.LUDO);
        history.setWinner(true);

        // Then
        Assert.assertEquals(history.getAccountId(), "user_test");
        Assert.assertEquals(history.getGameType(), GameType.LUDO);
        Assert.assertTrue(history.isWinner());
    }
}