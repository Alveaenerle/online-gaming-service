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

    @Test
    public void shouldPreventJoiningWhenFullOrNotWaiting() {
        GameRoom room = new GameRoom("R2", GameType.LUDO, "host", 2, false);
        room.addPlayer("p2");

        Assert.assertEquals(room.getStatus(), RoomStatus.FULL);
        Assert.expectThrows(IllegalStateException.class, () -> room.addPlayer("p3"));

        room.removePlayer("p2");
        room.setStatus(RoomStatus.PLAYING);
        Assert.expectThrows(IllegalStateException.class, () -> room.addPlayer("p3"));
    }

    @Test
    public void shouldPromoteNextPlayerToHostWhenCurrentLeaves() {
        GameRoom room = new GameRoom("R3", GameType.MAKAO, "host", 4, false);
        room.addPlayer("p2");
        room.addPlayer("p3");

        room.removePlayer("host");

        Assert.assertEquals(room.getHostUsername(), "p2");
        Assert.assertEquals(room.getPlayersUsernames().size(), 2);
    }

    @Test
    public void shouldNotDuplicatePlayersWhenAddingExisting() {
        GameRoom room = new GameRoom("R4", GameType.LUDO, "host", 3, false);

        room.addPlayer("host");
        room.addPlayer("host");

        Assert.assertEquals(room.getPlayersUsernames().size(), 1);
    }

    @Test
    public void shouldHandleNullPlayersList() {
        GameRoom room = new GameRoom("R5", GameType.MAKAO, "host", 3, false);

        room.setPlayersUsernames(null);

        Assert.assertNotNull(room.getPlayersUsernames());
        Assert.assertTrue(room.getPlayersUsernames().isEmpty());
    }
}