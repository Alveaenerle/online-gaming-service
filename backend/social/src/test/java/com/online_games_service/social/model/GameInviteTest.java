package com.online_games_service.social.model;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.Instant;

/**
 * Unit tests for GameInvite model.
 */
public class GameInviteTest {

    @Test
    public void create_ShouldInitializeAllFields() {
        // Given
        String senderId = "sender123";
        String senderUsername = "Alice";
        String targetId = "target456";
        String lobbyId = "lobby789";
        String lobbyName = "Fun Game Room";
        String gameType = "MAKAO";
        String accessCode = "ABC123";

        // When
        GameInvite invite = GameInvite.create(senderId, senderUsername, targetId, lobbyId, lobbyName, gameType, accessCode);

        // Then
        Assert.assertNotNull(invite.getId());
        Assert.assertEquals(invite.getSenderId(), senderId);
        Assert.assertEquals(invite.getSenderUsername(), senderUsername);
        Assert.assertEquals(invite.getTargetId(), targetId);
        Assert.assertEquals(invite.getLobbyId(), lobbyId);
        Assert.assertEquals(invite.getLobbyName(), lobbyName);
        Assert.assertEquals(invite.getGameType(), gameType);
        Assert.assertEquals(invite.getAccessCode(), accessCode);
        Assert.assertTrue(invite.getCreatedAt() > 0);
    }

    @Test
    public void create_ShouldGenerateUniqueIds() {
        // Given/When
        GameInvite invite1 = GameInvite.create("s1", "u1", "t1", "l1", "name1", "MAKAO", "CODE1");
        GameInvite invite2 = GameInvite.create("s2", "u2", "t2", "l2", "name2", "LUDO", "CODE2");

        // Then
        Assert.assertNotEquals(invite1.getId(), invite2.getId());
    }

    @Test
    public void getCreatedAtInstant_ShouldReturnCorrectInstant() {
        // Given
        long now = System.currentTimeMillis();
        GameInvite invite = GameInvite.builder()
                .id("test-id")
                .senderId("sender")
                .targetId("target")
                .lobbyId("lobby")
                .createdAt(now)
                .build();

        // When
        Instant instant = invite.getCreatedAtInstant();

        // Then
        Assert.assertNotNull(instant);
        Assert.assertEquals(instant.toEpochMilli(), now);
    }

    @Test
    public void builder_ShouldBuildCorrectly() {
        // Given/When
        GameInvite invite = GameInvite.builder()
                .id("invite-id")
                .senderId("sender123")
                .senderUsername("TestUser")
                .targetId("target456")
                .lobbyId("lobby789")
                .lobbyName("Test Lobby")
                .gameType("LUDO")
                .createdAt(1234567890L)
                .build();

        // Then
        Assert.assertEquals(invite.getId(), "invite-id");
        Assert.assertEquals(invite.getSenderId(), "sender123");
        Assert.assertEquals(invite.getSenderUsername(), "TestUser");
        Assert.assertEquals(invite.getTargetId(), "target456");
        Assert.assertEquals(invite.getLobbyId(), "lobby789");
        Assert.assertEquals(invite.getLobbyName(), "Test Lobby");
        Assert.assertEquals(invite.getGameType(), "LUDO");
        Assert.assertEquals(invite.getCreatedAt(), 1234567890L);
    }

    @Test
    public void noArgsConstructor_ShouldCreateEmptyInvite() {
        // When
        GameInvite invite = new GameInvite();

        // Then
        Assert.assertNull(invite.getId());
        Assert.assertNull(invite.getSenderId());
        Assert.assertNull(invite.getTargetId());
    }

    @Test
    public void allArgsConstructor_ShouldSetAllFields() {
        // When
        GameInvite invite = new GameInvite(
                "id1", "sender", "senderName", "target", 
                "lobby", "lobbyName", "MAKAO", "ABC123", 12345L);

        // Then
        Assert.assertEquals(invite.getId(), "id1");
        Assert.assertEquals(invite.getSenderId(), "sender");
        Assert.assertEquals(invite.getSenderUsername(), "senderName");
        Assert.assertEquals(invite.getTargetId(), "target");
        Assert.assertEquals(invite.getLobbyId(), "lobby");
        Assert.assertEquals(invite.getLobbyName(), "lobbyName");
        Assert.assertEquals(invite.getGameType(), "MAKAO");
        Assert.assertEquals(invite.getAccessCode(), "ABC123");
        Assert.assertEquals(invite.getCreatedAt(), 12345L);
    }

    @Test
    public void setters_ShouldModifyFields() {
        // Given
        GameInvite invite = new GameInvite();

        // When
        invite.setId("newId");
        invite.setSenderId("newSender");
        invite.setSenderUsername("NewSenderName");
        invite.setTargetId("newTarget");
        invite.setLobbyId("newLobby");
        invite.setLobbyName("New Lobby Name");
        invite.setGameType("LUDO");
        invite.setCreatedAt(99999L);

        // Then
        Assert.assertEquals(invite.getId(), "newId");
        Assert.assertEquals(invite.getSenderId(), "newSender");
        Assert.assertEquals(invite.getSenderUsername(), "NewSenderName");
        Assert.assertEquals(invite.getTargetId(), "newTarget");
        Assert.assertEquals(invite.getLobbyId(), "newLobby");
        Assert.assertEquals(invite.getLobbyName(), "New Lobby Name");
        Assert.assertEquals(invite.getGameType(), "LUDO");
        Assert.assertEquals(invite.getCreatedAt(), 99999L);
    }

    @Test
    public void equals_ShouldReturnTrueForSameData() {
        // Given
        GameInvite invite1 = GameInvite.builder()
                .id("same-id")
                .senderId("sender")
                .targetId("target")
                .lobbyId("lobby")
                .build();
        
        GameInvite invite2 = GameInvite.builder()
                .id("same-id")
                .senderId("sender")
                .targetId("target")
                .lobbyId("lobby")
                .build();

        // Then
        Assert.assertEquals(invite1, invite2);
        Assert.assertEquals(invite1.hashCode(), invite2.hashCode());
    }

    @Test
    public void equals_ShouldReturnFalseForDifferentData() {
        // Given
        GameInvite invite1 = GameInvite.builder().id("id1").build();
        GameInvite invite2 = GameInvite.builder().id("id2").build();

        // Then
        Assert.assertNotEquals(invite1, invite2);
    }

    @Test
    public void toString_ShouldContainRelevantFields() {
        // Given
        GameInvite invite = GameInvite.builder()
                .id("test-id")
                .senderId("sender")
                .targetId("target")
                .lobbyId("lobby")
                .build();

        // When
        String result = invite.toString();

        // Then
        Assert.assertTrue(result.contains("test-id"));
        Assert.assertTrue(result.contains("sender"));
        Assert.assertTrue(result.contains("target"));
        Assert.assertTrue(result.contains("lobby"));
    }
}
