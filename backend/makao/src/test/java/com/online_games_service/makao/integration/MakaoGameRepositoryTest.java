package com.online_games_service.makao.integration;

import com.online_games_service.common.enums.CardRank;
import com.online_games_service.common.enums.CardSuit;
import com.online_games_service.common.enums.RoomStatus;
import com.online_games_service.common.model.Card;
import com.online_games_service.makao.model.MakaoGame;
import com.online_games_service.makao.repository.redis.MakaoGameRedisRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class MakaoGameRepositoryTest extends AbstractTestNGSpringContextTests {

    @Autowired
    private MakaoGameRedisRepository repository;

    @AfterMethod
    public void cleanUp() {
        repository.deleteAll();
    }

    @Test(description = "Should save and retrieve full game state from Redis (JSON serialization verification)")
    public void shouldSaveAndRetrieveComplexGameState() {
        // Given
        String roomId = "integration_test_1";
        String player1 = "jan_kowalski";
        String player2 = "anna_nowak";
        
        MakaoGame game = new MakaoGame(roomId, List.of(player1, player2));

        game.setStatus(RoomStatus.FINISHED);
        game.setWinnerId(player1);
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
        assertThat(loadedGame.getWinnerId()).isEqualTo(player1);

        assertThat(loadedGame.getDemandedRank()).isEqualTo(CardRank.JACK);
        assertThat(loadedGame.getDemandedSuit()).isEqualTo(CardSuit.DIAMONDS);

        assertThat(loadedGame.getPlayersHands()).containsKey(player1);
        List<Card> player1Hand = loadedGame.getPlayersHands().get(player1);
        
        assertThat(player1Hand).hasSize(2);
        assertThat(player1Hand.get(0).getSuit()).isEqualTo(CardSuit.HEARTS); 
        assertThat(player1Hand.get(0).getRank()).isEqualTo(CardRank.ACE);
    }

    @Test(description = "Should update existing game in Redis")
    public void shouldUpdateExistingGame() {
        // Given
        String roomId = "update_test";
        MakaoGame game = new MakaoGame(roomId, List.of("p1"));
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
        MakaoGame game = new MakaoGame(roomId, List.of("p1"));
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
        MakaoGame game1 = new MakaoGame("count_test_1", List.of("p1"));
        MakaoGame game2 = new MakaoGame("count_test_2", List.of("p2"));
        repository.save(game1);
        repository.save(game2);

        // When
        long count = repository.count();

        // Then
        assertThat(count).isEqualTo(2);
    }
}