package com.online_games_service.social.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.online_games_service.social.dto.GameInviteDto;
import com.online_games_service.social.exception.GameInviteException;
import com.online_games_service.social.model.GameInvite;
import com.online_games_service.social.repository.GameInviteRedisRepository;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;

import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for GameInviteService.
 */
public class GameInviteServiceTest {

    private GameInviteService gameInviteService;
    private GameInviteRedisRepository gameInviteRepository;
    private PresenceService presenceService;
    private FriendNotificationService friendNotificationService;
    private StringRedisTemplate stringRedisTemplate;
    private ValueOperations<String, String> valueOperations;
    private ObjectMapper objectMapper;

    @BeforeMethod
    @SuppressWarnings("unchecked")
    public void setUp() {
        gameInviteRepository = mock(GameInviteRedisRepository.class);
        presenceService = mock(PresenceService.class);
        friendNotificationService = mock(FriendNotificationService.class);
        stringRedisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        objectMapper = new ObjectMapper();

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        gameInviteService = new GameInviteService(
                gameInviteRepository,
                presenceService,
                friendNotificationService,
                stringRedisTemplate,
                objectMapper
        );
    }

    // ============================================================
    // SEND GAME INVITE - SUCCESS TESTS
    // ============================================================

    @Test
    public void sendGameInvite_Success_SavesAndReturnsDto() {
        // Given
        String senderId = "sender1";
        String senderUsername = "Alice";
        String targetId = "target1";
        String lobbyId = "lobby1";
        String lobbyName = "Fun Room";
        String gameType = "MAKAO";

        when(gameInviteRepository.existsBySenderIdAndTargetIdAndLobbyId(senderId, targetId, lobbyId))
                .thenReturn(false);
        
        String roomJson = "{\"status\":\"WAITING\"}";
        when(valueOperations.get("game:room:" + lobbyId)).thenReturn(roomJson);
        
        when(gameInviteRepository.save(any(GameInvite.class))).thenAnswer(inv -> inv.getArgument(0));
        when(presenceService.isUserOnline(targetId)).thenReturn(false);

        // When
        GameInviteDto result = gameInviteService.sendGameInvite(
                senderId, senderUsername, targetId, lobbyId, lobbyName, gameType);

        // Then
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getSenderId(), senderId);
        Assert.assertEquals(result.getSenderUsername(), senderUsername);
        Assert.assertEquals(result.getTargetId(), targetId);
        Assert.assertEquals(result.getLobbyId(), lobbyId);
        Assert.assertEquals(result.getLobbyName(), lobbyName);
        Assert.assertEquals(result.getGameType(), gameType);

        verify(gameInviteRepository).save(any(GameInvite.class));
    }

    @Test
    public void sendGameInvite_TargetOnline_SendsNotification() {
        // Given
        String senderId = "sender1";
        String targetId = "target1";
        String lobbyId = "lobby1";

        when(gameInviteRepository.existsBySenderIdAndTargetIdAndLobbyId(any(), any(), any()))
                .thenReturn(false);
        
        String roomJson = "{\"status\":\"WAITING\",\"accessCode\":\"ABC123\"}";
        when(valueOperations.get("game:room:" + lobbyId)).thenReturn(roomJson);
        
        when(gameInviteRepository.save(any(GameInvite.class))).thenAnswer(inv -> inv.getArgument(0));
        when(presenceService.isUserOnline(targetId)).thenReturn(true);

        // When
        gameInviteService.sendGameInvite(senderId, "Sender", targetId, lobbyId, "Lobby", "MAKAO");

        // Then
        verify(friendNotificationService).sendGameInviteNotification(
                eq(targetId), any(), eq(senderId), eq("Sender"), 
                eq(lobbyId), eq("Lobby"), eq("MAKAO"), eq("ABC123"));
    }

    @Test
    public void sendGameInvite_TargetOffline_DoesNotSendNotification() {
        // Given
        String senderId = "sender1";
        String targetId = "target1";
        String lobbyId = "lobby1";

        when(gameInviteRepository.existsBySenderIdAndTargetIdAndLobbyId(any(), any(), any()))
                .thenReturn(false);
        
        String roomJson = "{\"status\":\"WAITING\"}";
        when(valueOperations.get("game:room:" + lobbyId)).thenReturn(roomJson);
        
        when(gameInviteRepository.save(any(GameInvite.class))).thenAnswer(inv -> inv.getArgument(0));
        when(presenceService.isUserOnline(targetId)).thenReturn(false);

        // When
        gameInviteService.sendGameInvite(senderId, "Sender", targetId, lobbyId, "Lobby", "MAKAO");

        // Then
        verify(friendNotificationService, never()).sendGameInviteNotification(
                any(), any(), any(), any(), any(), any(), any(), any());
    }

    // ============================================================
    // SEND GAME INVITE - ERROR TESTS
    // ============================================================

    @Test(expectedExceptions = GameInviteException.class)
    public void sendGameInvite_SelfInvite_ThrowsException() {
        // Given
        String userId = "user1";

        // When
        gameInviteService.sendGameInvite(userId, "User", userId, "lobby1", "Lobby", "MAKAO");
    }

    @Test
    public void sendGameInvite_SelfInvite_HasCorrectErrorCode() {
        // Given
        String userId = "user1";

        try {
            gameInviteService.sendGameInvite(userId, "User", userId, "lobby1", "Lobby", "MAKAO");
            Assert.fail("Should have thrown exception");
        } catch (GameInviteException e) {
            Assert.assertEquals(e.getErrorCode(), GameInviteException.ErrorCode.SELF_INVITE);
        }
    }

    @Test(expectedExceptions = GameInviteException.class)
    public void sendGameInvite_DuplicateInvite_ThrowsException() {
        // Given
        when(gameInviteRepository.existsBySenderIdAndTargetIdAndLobbyId("s1", "t1", "l1"))
                .thenReturn(true);

        // When
        gameInviteService.sendGameInvite("s1", "Sender", "t1", "l1", "Lobby", "MAKAO");
    }

    @Test
    public void sendGameInvite_DuplicateInvite_HasCorrectErrorCode() {
        // Given
        when(gameInviteRepository.existsBySenderIdAndTargetIdAndLobbyId("s1", "t1", "l1"))
                .thenReturn(true);

        try {
            gameInviteService.sendGameInvite("s1", "Sender", "t1", "l1", "Lobby", "MAKAO");
            Assert.fail("Should have thrown exception");
        } catch (GameInviteException e) {
            Assert.assertEquals(e.getErrorCode(), GameInviteException.ErrorCode.INVITE_ALREADY_PENDING);
        }
    }

    @Test(expectedExceptions = GameInviteException.class)
    public void sendGameInvite_LobbyNotAvailable_ThrowsException() {
        // Given
        when(gameInviteRepository.existsBySenderIdAndTargetIdAndLobbyId(any(), any(), any()))
                .thenReturn(false);
        when(valueOperations.get("game:room:lobby1")).thenReturn(null);

        // When
        gameInviteService.sendGameInvite("s1", "Sender", "t1", "lobby1", "Lobby", "MAKAO");
    }

    @Test
    public void sendGameInvite_LobbyNotAvailable_HasCorrectErrorCode() {
        // Given
        when(gameInviteRepository.existsBySenderIdAndTargetIdAndLobbyId(any(), any(), any()))
                .thenReturn(false);
        when(valueOperations.get("game:room:lobby1")).thenReturn(null);

        try {
            gameInviteService.sendGameInvite("s1", "Sender", "t1", "lobby1", "Lobby", "MAKAO");
            Assert.fail("Should have thrown exception");
        } catch (GameInviteException e) {
            Assert.assertEquals(e.getErrorCode(), GameInviteException.ErrorCode.LOBBY_NOT_AVAILABLE);
        }
    }

    // ============================================================
    // GET PENDING INVITES TESTS
    // ============================================================

    @Test
    public void getPendingInvites_ReturnsValidInvitesOnly() {
        // Given
        String userId = "user1";
        GameInvite validInvite = GameInvite.builder()
                .id("inv1").senderId("s1").targetId(userId).lobbyId("lobby1").build();
        GameInvite invalidInvite = GameInvite.builder()
                .id("inv2").senderId("s2").targetId(userId).lobbyId("lobby2").build();

        when(gameInviteRepository.findByTargetId(userId))
                .thenReturn(Arrays.asList(validInvite, invalidInvite));
        
        // lobby1 is valid, lobby2 is not
        String validRoomJson = "{\"status\":\"WAITING\"}";
        when(valueOperations.get("game:room:lobby1")).thenReturn(validRoomJson);
        when(valueOperations.get("game:room:lobby2")).thenReturn(null);

        // When
        List<GameInviteDto> result = gameInviteService.getPendingInvites(userId);

        // Then
        Assert.assertEquals(result.size(), 1);
        Assert.assertEquals(result.get(0).getLobbyId(), "lobby1");
        
        // Verify invalid invite was deleted
        verify(gameInviteRepository).deleteById("inv2");
    }

    @Test
    public void getPendingInvites_EmptyList_ReturnsEmpty() {
        // Given
        when(gameInviteRepository.findByTargetId("user1")).thenReturn(Collections.emptyList());

        // When
        List<GameInviteDto> result = gameInviteService.getPendingInvites("user1");

        // Then
        Assert.assertTrue(result.isEmpty());
    }

    // ============================================================
    // ACCEPT INVITE TESTS
    // ============================================================

    @Test
    public void acceptInvite_Success_DeletesAndReturnsInvite() {
        // Given
        String userId = "user1";
        String inviteId = "inv1";
        GameInvite invite = GameInvite.builder()
                .id(inviteId).senderId("sender").targetId(userId).lobbyId("lobby1").build();

        when(gameInviteRepository.findById(inviteId)).thenReturn(Optional.of(invite));
        
        String roomJson = "{\"status\":\"WAITING\"}";
        when(valueOperations.get("game:room:lobby1")).thenReturn(roomJson);

        // When
        GameInviteDto result = gameInviteService.acceptInvite(userId, inviteId);

        // Then
        Assert.assertEquals(result.getId(), inviteId);
        verify(gameInviteRepository).deleteById(inviteId);
    }

    @Test(expectedExceptions = GameInviteException.class)
    public void acceptInvite_NotFound_ThrowsException() {
        // Given
        when(gameInviteRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // When
        gameInviteService.acceptInvite("user1", "nonexistent");
    }

    @Test(expectedExceptions = GameInviteException.class)
    public void acceptInvite_WrongUser_ThrowsException() {
        // Given
        GameInvite invite = GameInvite.builder()
                .id("inv1").targetId("otherUser").lobbyId("lobby1").build();
        when(gameInviteRepository.findById("inv1")).thenReturn(Optional.of(invite));

        // When
        gameInviteService.acceptInvite("wrongUser", "inv1");
    }

    @Test
    public void acceptInvite_LobbyNoLongerAvailable_DeletesAndThrows() {
        // Given
        String userId = "user1";
        String inviteId = "inv1";
        GameInvite invite = GameInvite.builder()
                .id(inviteId).targetId(userId).lobbyId("lobby1").build();

        when(gameInviteRepository.findById(inviteId)).thenReturn(Optional.of(invite));
        when(valueOperations.get("game:room:lobby1")).thenReturn(null);

        try {
            gameInviteService.acceptInvite(userId, inviteId);
            Assert.fail("Should have thrown exception");
        } catch (GameInviteException e) {
            Assert.assertEquals(e.getErrorCode(), GameInviteException.ErrorCode.LOBBY_NOT_AVAILABLE);
            verify(gameInviteRepository).deleteById(inviteId);
        }
    }

    // ============================================================
    // DECLINE INVITE TESTS
    // ============================================================

    @Test
    public void declineInvite_Success_DeletesAndReturnsInvite() {
        // Given
        String userId = "user1";
        String inviteId = "inv1";
        GameInvite invite = GameInvite.builder()
                .id(inviteId).senderId("sender").targetId(userId).lobbyId("lobby1").build();

        when(gameInviteRepository.findById(inviteId)).thenReturn(Optional.of(invite));

        // When
        GameInviteDto result = gameInviteService.declineInvite(userId, inviteId);

        // Then
        Assert.assertEquals(result.getId(), inviteId);
        verify(gameInviteRepository).deleteById(inviteId);
    }

    @Test(expectedExceptions = GameInviteException.class)
    public void declineInvite_NotFound_ThrowsException() {
        // Given
        when(gameInviteRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // When
        gameInviteService.declineInvite("user1", "nonexistent");
    }

    @Test(expectedExceptions = GameInviteException.class)
    public void declineInvite_WrongUser_ThrowsException() {
        // Given
        GameInvite invite = GameInvite.builder()
                .id("inv1").targetId("otherUser").build();
        when(gameInviteRepository.findById("inv1")).thenReturn(Optional.of(invite));

        // When
        gameInviteService.declineInvite("wrongUser", "inv1");
    }

    // ============================================================
    // GET SENT INVITES TESTS
    // ============================================================

    @Test
    public void getSentInvites_ReturnsAllSentInvites() {
        // Given
        String senderId = "sender1";
        GameInvite invite1 = GameInvite.builder()
                .id("inv1").senderId(senderId).targetId("t1").build();
        GameInvite invite2 = GameInvite.builder()
                .id("inv2").senderId(senderId).targetId("t2").build();

        when(gameInviteRepository.findBySenderId(senderId))
                .thenReturn(Arrays.asList(invite1, invite2));

        // When
        List<GameInviteDto> result = gameInviteService.getSentInvites(senderId);

        // Then
        Assert.assertEquals(result.size(), 2);
    }

    // ============================================================
    // DELETE INVITES FOR LOBBY TESTS
    // ============================================================

    @Test
    public void deleteInvitesForLobby_ReturnsDeleteCount() {
        // Given
        when(gameInviteRepository.deleteAllByLobbyId("lobby1")).thenReturn(5);

        // When
        int count = gameInviteService.deleteInvitesForLobby("lobby1");

        // Then
        Assert.assertEquals(count, 5);
    }

    // ============================================================
    // IS LOBBY JOINABLE TESTS
    // ============================================================

    @Test
    public void isLobbyJoinable_StatusWaiting_ReturnsTrue() {
        // Given
        String roomJson = "{\"status\":\"WAITING\"}";
        when(valueOperations.get("game:room:lobby1")).thenReturn(roomJson);

        // When
        boolean result = gameInviteService.isLobbyJoinable("lobby1");

        // Then
        Assert.assertTrue(result);
    }

    @Test
    public void isLobbyJoinable_StatusFull_ReturnsTrue() {
        // Given
        String roomJson = "{\"status\":\"FULL\"}";
        when(valueOperations.get("game:room:lobby1")).thenReturn(roomJson);

        // When
        boolean result = gameInviteService.isLobbyJoinable("lobby1");

        // Then
        Assert.assertTrue(result);
    }

    @Test
    public void isLobbyJoinable_StatusStarted_ReturnsFalse() {
        // Given
        String roomJson = "{\"status\":\"STARTED\"}";
        when(valueOperations.get("game:room:lobby1")).thenReturn(roomJson);

        // When
        boolean result = gameInviteService.isLobbyJoinable("lobby1");

        // Then
        Assert.assertFalse(result);
    }

    @Test
    public void isLobbyJoinable_LobbyNotFound_ReturnsFalse() {
        // Given
        when(valueOperations.get("game:room:nonexistent")).thenReturn(null);

        // When
        boolean result = gameInviteService.isLobbyJoinable("nonexistent");

        // Then
        Assert.assertFalse(result);
    }

    @Test
    public void isLobbyJoinable_ExceptionThrown_ReturnsFalse() {
        // Given
        when(valueOperations.get("game:room:error")).thenThrow(new RuntimeException("Redis error"));

        // When
        boolean result = gameInviteService.isLobbyJoinable("error");

        // Then
        Assert.assertFalse(result);
    }
}
