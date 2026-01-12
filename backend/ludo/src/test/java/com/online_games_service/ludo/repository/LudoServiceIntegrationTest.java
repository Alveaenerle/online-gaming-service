package com.online_games_service.ludo.repository;

import com.online_games_service.common.enums.RoomStatus;
import com.online_games_service.ludo.enums.PlayerColor;
import com.online_games_service.ludo.model.LudoGame;
import com.online_games_service.ludo.model.LudoPawn;
import com.online_games_service.ludo.model.LudoPlayer;
import com.online_games_service.ludo.repository.mongo.LudoGameResultRepository;
import com.online_games_service.ludo.repository.redis.LudoGameRedisRepository;
import com.online_games_service.ludo.service.LudoService;
import com.online_games_service.test.BaseIntegrationTest;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class LudoServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private LudoService ludoService;

    @Autowired
    private LudoGameRedisRepository redisRepository;

    @Autowired
    private LudoGameResultRepository mongoRepository;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    private final String ROOM_ID = "integration-room";
    private final String P1_ID = "user-1";
    private final String P2_ID = "user-2";

    @BeforeMethod
    public void cleanUp() {
        redisRepository.deleteById(ROOM_ID);
        mongoRepository.deleteAll();
    }

    @Test
    public void shouldPersistGameInRedisAfterCreation() {
        // Given & When
        ludoService.createGame(ROOM_ID, List.of(P1_ID, P2_ID), P1_ID, Map.of(P1_ID, "Alice", P2_ID, "Bob"));

        // Then
        Optional<LudoGame> gameOpt = redisRepository.findById(ROOM_ID);
        Assert.assertTrue(gameOpt.isPresent());
        
        LudoGame game = gameOpt.get();
        Assert.assertEquals(game.getStatus(), RoomStatus.PLAYING);
        Assert.assertEquals(game.getPlayers().size(), 2);
        Assert.assertEquals(game.getPlayers().get(0).getColor(), PlayerColor.RED);
        Assert.assertNotNull(game.getPlayers().get(0).getPawns());
    }

    @Test
    public void fullGameFlow_HappyPath() {
        // Given
        ludoService.createGame(ROOM_ID, List.of(P1_ID, P2_ID), P1_ID, Map.of(P1_ID, "Alice", P2_ID, "Bob"));
        
        // Simulate a rolled 6
        LudoGame game = redisRepository.findById(ROOM_ID).get();
        game.setDiceRolled(true);
        game.setLastDiceRoll(6); 
        game.setWaitingForMove(true);
        redisRepository.save(game); 

        // When
        ludoService.movePawn(P1_ID, 0);

        // Then
        LudoGame updatedGame = redisRepository.findById(ROOM_ID).get();
        LudoPawn pawn = updatedGame.getPlayers().get(0).getPawns().get(0);
        
        Assert.assertFalse(pawn.isInBase());
        Assert.assertEquals(updatedGame.getRollsLeft(), 1);
    }

    @Test
    public void shouldSaveResultToMongoOnFinish() {
        // Given
        ludoService.createGame(ROOM_ID, List.of(P1_ID, P2_ID), P1_ID, Map.of(P1_ID, "Alice", P2_ID, "Bob"));
        
        LudoGame game = redisRepository.findById(ROOM_ID).get();
        LudoPlayer winner = game.getPlayers().get(0);
        
        winner.getPawns().forEach(p -> { p.setInBase(false); p.setInHome(true); });
        
        LudoPawn lastPawn = winner.getPawns().get(3);
        lastPawn.setInHome(false);
        lastPawn.setStepsMoved(51); 
        lastPawn.setPosition(51);  
        
        game.setDiceRolled(true);
        game.setLastDiceRoll(1); 
        game.setWaitingForMove(true);
        redisRepository.save(game);

        // When
        ludoService.movePawn(P1_ID, 3);

        // Then
        Assert.assertFalse(redisRepository.existsById(ROOM_ID));
        Assert.assertEquals(mongoRepository.count(), 1);
        
        var result = mongoRepository.findAll().get(0);
        Assert.assertEquals(result.getWinnerId(), P1_ID);
    }
}