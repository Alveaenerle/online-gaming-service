package com.online_games_service.makao.integration;

import com.online_games_service.common.enums.CardRank;
import com.online_games_service.common.enums.CardSuit;
import com.online_games_service.common.enums.RoomStatus;
import com.online_games_service.common.model.Card;
import com.online_games_service.makao.model.MakaoGame;
import com.online_games_service.makao.repository.redis.MakaoGameRedisRepository;
import com.online_games_service.test.BaseIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class MakaoGameRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private MakaoGameRedisRepository repository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @AfterMethod
    public void cleanUp() {
        ScanOptions options = ScanOptions.scanOptions()
                .match(repository.getKeyPrefix() + "*")
                .count(1000)
                .build();
        
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                redisTemplate.delete(cursor.next());
            }
        }
    }

    @Test(description = "Should save and retrieve full game state from Redis (JSON serialization verification)")
    public void shouldSaveAndRetrieveComplexGameState() {
        // Given
        String roomId = "integration_test_1";
        String player1 = "jan_kowalski";
        String player2 = "anna_nowak";
        
        MakaoGame game = new MakaoGame(roomId, Map.of(player1, "Player 1", player2, "Player 2"), player1, 4);

        game.setStatus(RoomStatus.FINISHED);
        game.getRanking().put(player1, 0);
        game.setDemandedRank(CardRank.JACK);
        game.setDemandedSuit(CardSuit.DIAMONDS);

        Card aceHearts = new Card(CardSuit.HEARTS, CardRank.ACE);
        Card tenClubs = new Card(CardSuit.CLUBS, CardRank.TEN);
        
        game.addCardToHand(player1, aceHearts);
        game.addCardToHand(player1, tenClubs);

        game.addToDiscardPile(new Card(CardSuit.SPADES, CardRank.TWO));

        // When
        repository.save(game);

        // Then
        Optional<MakaoGame> loadedGameOpt = repository.findById(roomId);
        assertThat(loadedGameOpt).isPresent();

        MakaoGame loadedGame = loadedGameOpt.get();

        assertThat(loadedGame.getId()).isEqualTo(roomId);
        assertThat(loadedGame.getStatus()).isEqualTo(RoomStatus.FINISHED);
        assertThat(loadedGame.getRanking()).containsEntry(player1, 0);

        assertThat(loadedGame.getDemandedRank()).isEqualTo(CardRank.JACK);
        assertThat(loadedGame.getDemandedSuit()).isEqualTo(CardSuit.DIAMONDS);

        assertThat(loadedGame.getPlayersHands()).containsKey(player1);
        List<Card> player1Hand = loadedGame.getPlayersHands().get(player1);
        
        assertThat(player1Hand).hasSize(7);
        assertThat(player1Hand).contains(aceHearts, tenClubs);
    }

    @Test(description = "Should update existing game in Redis")
    public void shouldUpdateExistingGame() {
        // Given
        String roomId = "update_test";
        MakaoGame game = new MakaoGame(roomId, Map.of("p1", "P1"), "p1", 4);
        repository.save(game);

        // When
        MakaoGame fetched = repository.findById(roomId).get();
        fetched.setPendingDrawCount(5);
        fetched.setStatus(RoomStatus.FINISHED);
        repository.save(fetched);

        // Then
        MakaoGame updated = repository.findById(roomId).get();
        assertThat(updated.getPendingDrawCount()).isEqualTo(5);
        assertThat(updated.getStatus()).isEqualTo(RoomStatus.FINISHED);
    }

    @Test(description = "Should delete game from Redis")
    public void shouldDeleteGame() {
        // Given
        String roomId = "delete_test";
        MakaoGame game = new MakaoGame(roomId, Map.of("p1", "P1"), "p1", 4);
        repository.save(game);

        // When
        repository.deleteById(roomId);

        // Then
        Optional<MakaoGame> deletedGame = repository.findById(roomId);
        assertThat(deletedGame).isNotPresent();
    }
    @Test(description = "Should count games in Redis")
    public void shouldCountGames() {
        // Given
        MakaoGame game1 = new MakaoGame("count_test_1", Map.of("p1", "P1"), "p1", 4);
        MakaoGame game2 = new MakaoGame("count_test_2", Map.of("p2", "P2"), "p2", 4);
        repository.save(game1);
        repository.save(game2);

        // When
        long count = repository.countGames();

        // Then
        assertThat(count).isEqualTo(2);
    }
}