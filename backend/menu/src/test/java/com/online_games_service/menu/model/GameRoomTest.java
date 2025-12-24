package com.online_games_service.menu.model;

import com.online_games_service.common.enums.GameType;
import com.online_games_service.common.enums.RoomStatus;
import org.testng.Assert;
import org.testng.annotations.Test;

public class GameRoomTest {

    @Test
    public void shouldInitializeCorrectly() {
        // Given
        String hostUsername = "host_user";
        
        // When
        GameRoom room = new GameRoom("Test Room", GameType.LUDO, hostUsername, 4, false);

        // Then
        Assert.assertEquals(room.getStatus(), RoomStatus.WAITING);
        Assert.assertEquals(room.getPlayersUsernames().size(), 1, "Host should be added automatically");
        Assert.assertTrue(room.getPlayersUsernames().contains(hostUsername));
    }

    @Test
    public void testCanJoinLogic() {
        // Given
        GameRoom room = new GameRoom("R1", GameType.MAKAO, "host", 4, false);
        
        // When & Then
        Assert.assertTrue(room.canJoin(), "Should be able to join valid room");

        // When & Then
        room.setStatus(RoomStatus.PLAYING);
        Assert.assertFalse(room.canJoin(), "Should not join started game");

        // When & Then
        room.setStatus(RoomStatus.FINISHED);
        Assert.assertFalse(room.canJoin(), "Should not join finished game");

        // When & Then
        room.setStatus(RoomStatus.WAITING);
        room.addPlayer("p2");
        room.addPlayer("p3");
        room.addPlayer("p4");
        
        Assert.assertEquals(room.getPlayersUsernames().size(), 4);
        Assert.assertFalse(room.canJoin(), "Should not join full room");
    }

    @Test
    public void shouldThrowExceptionOnDirectSetModification() {
        // Given
        GameRoom room = new GameRoom("R1", GameType.MAKAO, "host", 4, false);
        
        // When & Then
        Assert.assertThrows(UnsupportedOperationException.class, () -> {
            room.getPlayersUsernames().add("hacker");
        });
    }

    @Test
    public void shouldManageStatusAutomatically() {
        // Given
        GameRoom room = new GameRoom("R1", GameType.MAKAO, "host", 2, false);
        
        // When
        room.addPlayer("p2");

        // Then
        Assert.assertEquals(room.getStatus(), RoomStatus.FULL);
        
        // When
        room.removePlayer("p2");
        
        // Then
        Assert.assertEquals(room.getStatus(), RoomStatus.WAITING);
    }
}