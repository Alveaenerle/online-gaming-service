package com.online_games_service.menu.model;

import com.online_games_service.common.enums.GameType;
import com.online_games_service.common.enums.RoomStatus;
import org.testng.Assert;
import org.testng.annotations.Test;

public class GameRoomTest {

    @Test
    public void shouldInitializeCorrectly() {
        // Given
        String hostUserId = "host-id";
        String hostUsername = "host_user";
        
        // When
        GameRoom room = new GameRoom("Test Room", GameType.LUDO, hostUserId, hostUsername, 4, false);

        // Then
        Assert.assertEquals(room.getStatus(), RoomStatus.WAITING);
        Assert.assertEquals(room.getPlayers().size(), 1, "Host should be added automatically");
        Assert.assertEquals(room.getPlayers().get(hostUserId), hostUsername);
    }

    @Test
    public void testCanJoinLogic() {
        // Given
        GameRoom room = new GameRoom("R1", GameType.MAKAO, "host-id", "host", 4, false);
        
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
        room.addPlayer("p2-id", "p2");
        room.addPlayer("p3-id", "p3");
        room.addPlayer("p4-id", "p4");
		
        Assert.assertEquals(room.getPlayers().size(), 4);
        Assert.assertFalse(room.canJoin(), "Should not join full room");
    }

    @Test
    public void shouldManageStatusAutomatically() {
        // Given
        GameRoom room = new GameRoom("R1", GameType.MAKAO, "host-id", "host", 2, false);
        
        // When
        room.addPlayer("p2-id", "p2");

        // Then
        Assert.assertEquals(room.getStatus(), RoomStatus.FULL);
        
        // When
        room.removePlayerById("p2-id");
        
        // Then
        Assert.assertEquals(room.getStatus(), RoomStatus.WAITING);
    }

    @Test
    public void shouldPreventJoiningWhenFullOrNotWaiting() {
        GameRoom room = new GameRoom("R2", GameType.LUDO, "host-id", "host", 2, false);
        room.addPlayer("p2-id", "p2");

        Assert.assertEquals(room.getStatus(), RoomStatus.FULL);
        Assert.expectThrows(IllegalStateException.class, () -> room.addPlayer("p3-id", "p3"));

        room.removePlayerById("p2-id");
        room.setStatus(RoomStatus.PLAYING);
        Assert.expectThrows(IllegalStateException.class, () -> room.addPlayer("p3-id", "p3"));
    }

    @Test
    public void shouldPromoteNextPlayerToHostWhenCurrentLeaves() {
        GameRoom room = new GameRoom("R3", GameType.MAKAO, "host-id", "host", 4, false);
        room.addPlayer("p2-id", "p2");
        room.addPlayer("p3-id", "p3");

        room.removePlayerById("host-id");

        Assert.assertEquals(room.getHostUserId(), "p2-id");
        Assert.assertEquals(room.getHostUsername(), "p2");
        Assert.assertEquals(room.getPlayers().size(), 2);
    }

    @Test
    public void shouldNotDuplicatePlayersWhenAddingExisting() {
        GameRoom room = new GameRoom("R4", GameType.LUDO, "host-id", "host", 3, false);

        room.addPlayer("host-id", "host");
        room.addPlayer("host-id", "host");

        Assert.assertEquals(room.getPlayers().size(), 1);
    }

    @Test
    public void shouldHandleNullPlayersList() {
        GameRoom room = new GameRoom("R5", GameType.MAKAO, "host-id", "host", 3, false);

        room.setPlayers(new java.util.LinkedHashMap<>());

        Assert.assertNotNull(room.getPlayers());
        Assert.assertTrue(room.getPlayers().isEmpty());
    }
}