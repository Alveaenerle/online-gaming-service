package com.online_games_service.social.controller;

import com.online_games_service.social.dto.GameInviteDto;
import com.online_games_service.social.dto.RespondGameInviteRequest;
import com.online_games_service.social.dto.SendGameInviteRequest;
import com.online_games_service.social.exception.GameInviteException;
import com.online_games_service.social.service.GameInviteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

/**
 * Unit tests for GameInviteController.
 */
public class GameInviteControllerTest {

    private GameInviteController controller;
    private GameInviteService gameInviteService;

    @BeforeMethod
    public void setUp() {
        gameInviteService = mock(GameInviteService.class);
        controller = new GameInviteController(gameInviteService);
    }

    // ============================================================
    // SEND GAME INVITE TESTS
    // ============================================================

    @Test
    public void sendGameInvite_Success_Returns201() {
        // Given
        String userId = "user1";
        String username = "Alice";
        SendGameInviteRequest request = new SendGameInviteRequest("target1", "lobby1", "Fun Room", "MAKAO");

        GameInviteDto expectedDto = GameInviteDto.builder()
                .id("inv1")
                .senderId(userId)
                .targetId("target1")
                .lobbyId("lobby1")
                .build();

        when(gameInviteService.sendGameInvite(userId, username, "target1", "lobby1", "Fun Room", "MAKAO"))
                .thenReturn(expectedDto);

        // When
        ResponseEntity<?> response = controller.sendGameInvite(request, userId, username);

        // Then
        Assert.assertEquals(response.getStatusCode(), HttpStatus.CREATED);
        Assert.assertNotNull(response.getBody());
    }

    @Test
    public void sendGameInvite_NoUserId_Returns401() {
        // Given
        SendGameInviteRequest request = new SendGameInviteRequest("target1", "lobby1", "Room", "MAKAO");

        // When
        ResponseEntity<?> response = controller.sendGameInvite(request, null, null);

        // Then
        Assert.assertEquals(response.getStatusCode(), HttpStatus.UNAUTHORIZED);
        verify(gameInviteService, never()).sendGameInvite(any(), any(), any(), any(), any(), any());
    }

    @Test
    public void sendGameInvite_NoUsername_FallsBackToUserId() {
        // Given
        String userId = "user1";
        SendGameInviteRequest request = new SendGameInviteRequest("target1", "lobby1", "Room", "MAKAO");

        GameInviteDto expectedDto = GameInviteDto.builder().id("inv1").build();
        when(gameInviteService.sendGameInvite(userId, userId, "target1", "lobby1", "Room", "MAKAO"))
                .thenReturn(expectedDto);

        // When
        ResponseEntity<?> response = controller.sendGameInvite(request, userId, null);

        // Then
        Assert.assertEquals(response.getStatusCode(), HttpStatus.CREATED);
        verify(gameInviteService).sendGameInvite(userId, userId, "target1", "lobby1", "Room", "MAKAO");
    }

    // ============================================================
    // GET PENDING INVITES TESTS
    // ============================================================

    @Test
    public void getPendingInvites_Success_Returns200WithList() {
        // Given
        String userId = "user1";
        List<GameInviteDto> invites = Arrays.asList(
                GameInviteDto.builder().id("inv1").build(),
                GameInviteDto.builder().id("inv2").build()
        );
        when(gameInviteService.getPendingInvites(userId)).thenReturn(invites);

        // When
        ResponseEntity<List<GameInviteDto>> response = controller.getPendingInvites(userId);

        // Then
        Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
        Assert.assertEquals(response.getBody().size(), 2);
    }

    @Test
    public void getPendingInvites_NoUserId_Returns401() {
        // When
        ResponseEntity<List<GameInviteDto>> response = controller.getPendingInvites(null);

        // Then
        Assert.assertEquals(response.getStatusCode(), HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void getPendingInvites_EmptyList_Returns200() {
        // Given
        when(gameInviteService.getPendingInvites("user1")).thenReturn(Collections.emptyList());

        // When
        ResponseEntity<List<GameInviteDto>> response = controller.getPendingInvites("user1");

        // Then
        Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
        Assert.assertTrue(response.getBody().isEmpty());
    }

    // ============================================================
    // ACCEPT INVITE TESTS
    // ============================================================

    @Test
    public void acceptInvite_Success_Returns200() {
        // Given
        String userId = "user1";
        RespondGameInviteRequest request = new RespondGameInviteRequest("inv1");
        GameInviteDto expectedDto = GameInviteDto.builder()
                .id("inv1").lobbyId("lobby1").build();

        when(gameInviteService.acceptInvite(userId, "inv1")).thenReturn(expectedDto);

        // When
        ResponseEntity<?> response = controller.acceptInvite(request, userId);

        // Then
        Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
        Assert.assertNotNull(response.getBody());
    }

    @Test
    public void acceptInvite_NoUserId_Returns401() {
        // Given
        RespondGameInviteRequest request = new RespondGameInviteRequest("inv1");

        // When
        ResponseEntity<?> response = controller.acceptInvite(request, null);

        // Then
        Assert.assertEquals(response.getStatusCode(), HttpStatus.UNAUTHORIZED);
    }

    // ============================================================
    // DECLINE INVITE TESTS
    // ============================================================

    @Test
    public void declineInvite_Success_Returns200() {
        // Given
        String userId = "user1";
        RespondGameInviteRequest request = new RespondGameInviteRequest("inv1");
        GameInviteDto expectedDto = GameInviteDto.builder().id("inv1").build();

        when(gameInviteService.declineInvite(userId, "inv1")).thenReturn(expectedDto);

        // When
        ResponseEntity<?> response = controller.declineInvite(request, userId);

        // Then
        Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
    }

    @Test
    public void declineInvite_NoUserId_Returns401() {
        // Given
        RespondGameInviteRequest request = new RespondGameInviteRequest("inv1");

        // When
        ResponseEntity<?> response = controller.declineInvite(request, null);

        // Then
        Assert.assertEquals(response.getStatusCode(), HttpStatus.UNAUTHORIZED);
    }

    // ============================================================
    // GET SENT INVITES TESTS
    // ============================================================

    @Test
    public void getSentInvites_Success_Returns200WithList() {
        // Given
        String userId = "user1";
        List<GameInviteDto> invites = Arrays.asList(
                GameInviteDto.builder().id("inv1").build()
        );
        when(gameInviteService.getSentInvites(userId)).thenReturn(invites);

        // When
        ResponseEntity<List<GameInviteDto>> response = controller.getSentInvites(userId);

        // Then
        Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
        Assert.assertEquals(response.getBody().size(), 1);
    }

    @Test
    public void getSentInvites_NoUserId_Returns401() {
        // When
        ResponseEntity<List<GameInviteDto>> response = controller.getSentInvites(null);

        // Then
        Assert.assertEquals(response.getStatusCode(), HttpStatus.UNAUTHORIZED);
    }

    // ============================================================
    // EXCEPTION HANDLER TESTS
    // ============================================================

    @Test
    public void handleGameInviteException_SelfInvite_Returns400() {
        // Given
        GameInviteException ex = new GameInviteException(GameInviteException.ErrorCode.SELF_INVITE);

        // When
        ResponseEntity<Map<String, String>> response = controller.handleGameInviteException(ex);

        // Then
        Assert.assertEquals(response.getStatusCode(), HttpStatus.BAD_REQUEST);
        Assert.assertEquals(response.getBody().get("error"), "SELF_INVITE");
    }

    @Test
    public void handleGameInviteException_AlreadyPending_Returns400() {
        // Given
        GameInviteException ex = new GameInviteException(GameInviteException.ErrorCode.INVITE_ALREADY_PENDING);

        // When
        ResponseEntity<Map<String, String>> response = controller.handleGameInviteException(ex);

        // Then
        Assert.assertEquals(response.getStatusCode(), HttpStatus.BAD_REQUEST);
        Assert.assertEquals(response.getBody().get("error"), "INVITE_ALREADY_PENDING");
    }

    @Test
    public void handleGameInviteException_NotFound_Returns404() {
        // Given
        GameInviteException ex = new GameInviteException(GameInviteException.ErrorCode.INVITE_NOT_FOUND);

        // When
        ResponseEntity<Map<String, String>> response = controller.handleGameInviteException(ex);

        // Then
        Assert.assertEquals(response.getStatusCode(), HttpStatus.NOT_FOUND);
        Assert.assertEquals(response.getBody().get("error"), "INVITE_NOT_FOUND");
    }

    @Test
    public void handleGameInviteException_LobbyNotAvailable_Returns409() {
        // Given
        GameInviteException ex = new GameInviteException(GameInviteException.ErrorCode.LOBBY_NOT_AVAILABLE);

        // When
        ResponseEntity<Map<String, String>> response = controller.handleGameInviteException(ex);

        // Then
        Assert.assertEquals(response.getStatusCode(), HttpStatus.CONFLICT);
        Assert.assertEquals(response.getBody().get("error"), "LOBBY_NOT_AVAILABLE");
    }
}
