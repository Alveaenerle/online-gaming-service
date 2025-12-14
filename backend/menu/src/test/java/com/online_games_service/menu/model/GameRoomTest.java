package com.online_games_service.menu.model;

import com.online_games_service.common.enums.GameType;
import com.online_games_service.common.enums.RoomStatus;
import org.testng.Assert;
import org.testng.annotations.Test;

public class GameRoomTest {

    @Test
    public void shouldInitializeCorrectly() {
        // Given
        String hostId = "host_1";
        
        // When
        GameRoom room = new GameRoom("Test Room", GameType.LUDO, hostId, 4, false);

        // Then
        Assert.assertEquals(room.getStatus(), RoomStatus.WAITING);
        Assert.assertEquals(room.getPlayerIds().size(), 1, "Host should be added automatically");
        Assert.assertTrue(room.getPlayerIds().contains(hostId));
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
        room.getPlayerIds().add("p2");
        room.getPlayerIds().add("p3");
        room.getPlayerIds().add("p4");
        
        Assert.assertEquals(room.getPlayerIds().size(), 4);
        Assert.assertFalse(room.canJoin(), "Should not join full room");
    }

    @Test
    public void testLombokEquality() {
        // Given
        GameRoom r1 = new GameRoom("Room A", GameType.MAKAO, "h1", 2, true);
        GameRoom r2 = new GameRoom("Room A", GameType.MAKAO, "h1", 2, true);
        
        // Then
        Assert.assertEquals(r1, r2);
    }
}