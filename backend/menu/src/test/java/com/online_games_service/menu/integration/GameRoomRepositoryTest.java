package com.online_games_service.menu.integration;

import com.online_games_service.common.enums.GameType;
import com.online_games_service.common.enums.RoomStatus;
import com.online_games_service.menu.model.GameRoom;
import com.online_games_service.menu.repository.GameRoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Optional;

@SpringBootTest
@ActiveProfiles("test")
public class GameRoomRepositoryTest extends AbstractTestNGSpringContextTests {

    @Autowired
    private GameRoomRepository gameRoomRepository;

    @BeforeMethod
    public void cleanUp() {
        gameRoomRepository.deleteAll();
    }

    @Test
    public void shouldSaveAndRetrieveRoom() {
        // Given
        GameRoom room = new GameRoom("Test Room", GameType.LUDO, "host1", 4, false);
        
        // When
        GameRoom saved = gameRoomRepository.save(room);

        // Then
        Assert.assertNotNull(saved.getId());
        Optional<GameRoom> fetched = gameRoomRepository.findById(saved.getId());
        Assert.assertTrue(fetched.isPresent());
        Assert.assertEquals(fetched.get().getName(), "Test Room");
        Assert.assertNotNull(fetched.get().getCreatedAt(), "Auditing should work and set CreatedAt");
    }

    @Test
    public void shouldFilterRoomsByTypeAndStatus() {
        // Given
        gameRoomRepository.save(new GameRoom("Ludo Open", GameType.LUDO, "h1", 4, false));
        gameRoomRepository.save(new GameRoom("Makao Open", GameType.MAKAO, "h2", 4, false));
        
        GameRoom startedRoom = new GameRoom("Ludo Started", GameType.LUDO, "h3", 4, false);
        startedRoom.setStatus(RoomStatus.PLAYING);
        gameRoomRepository.save(startedRoom);

        // When
        List<GameRoom> result = gameRoomRepository.findAllByGameTypeAndStatus(GameType.LUDO, RoomStatus.WAITING);

        // Then
        Assert.assertEquals(result.size(), 1);
        Assert.assertEquals(result.get(0).getName(), "Ludo Open");
    }

    @Test
    public void shouldFindAllActiveRooms() {
        // Given
        gameRoomRepository.save(new GameRoom("R1", GameType.MAKAO, "h1", 2, false));
        gameRoomRepository.save(new GameRoom("R2", GameType.LUDO, "h2", 4, false));
        
        GameRoom finishedRoom = new GameRoom("R3", GameType.MAKAO, "h3", 2, false);
        finishedRoom.setStatus(RoomStatus.FINISHED);
        gameRoomRepository.save(finishedRoom);

        // When
        List<GameRoom> waitingRooms = gameRoomRepository.findAllByStatus(RoomStatus.WAITING);

        // Then
        Assert.assertEquals(waitingRooms.size(), 2);
    }
}