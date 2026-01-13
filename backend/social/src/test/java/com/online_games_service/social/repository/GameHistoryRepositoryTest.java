package com.online_games_service.social.repository;

import com.online_games_service.common.enums.GameType;
import com.online_games_service.social.model.GameHistory;
import com.online_games_service.social.repository.GameHistoryRepository;
import com.online_games_service.test.BaseIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

@SpringBootTest
public class GameHistoryRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private GameHistoryRepository gameHistoryRepository;

    @BeforeMethod
    public void cleanUp() {
        gameHistoryRepository.deleteAll();
    }

    @Test
    public void shouldSaveAndRetrieveHistory() {
        // Given
        String userId = "user_JP2";
        gameHistoryRepository.save(new GameHistory(userId, GameType.LUDO, "match_1", true));
        gameHistoryRepository.save(new GameHistory(userId, GameType.MAKAO, "match_2", false));
        
        gameHistoryRepository.save(new GameHistory("other_user", GameType.LUDO, "match_3", true));

        // When
        List<GameHistory> myHistory = gameHistoryRepository.findAllByAccountId(userId);
        List<GameHistory> myLudoHistory = gameHistoryRepository.findAllByAccountIdAndGameType(userId, GameType.LUDO);

        // Then
        Assert.assertEquals(myHistory.size(), 2);
        Assert.assertEquals(myLudoHistory.size(), 1);
        Assert.assertEquals(myLudoHistory.get(0).getMatchId(), "match_1");
    }
}