package com.online_games_service.social.controller;

import com.online_games_service.social.dto.AcceptFriendRequestDto;
import com.online_games_service.social.dto.FriendRequestResponseDto;
import com.online_games_service.social.dto.SendFriendRequestDto;
import com.online_games_service.social.exception.FriendRequestException;
import com.online_games_service.social.exception.FriendRequestException.ErrorCode;
import com.online_games_service.social.model.FriendRequest;
import com.online_games_service.social.service.FriendRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

import static org.mockito.Mockito.*;

/**
 * Unit tests for FriendRequestController.
 * Tests authentication via request attributes (userId, username) set by SessionUserFilter.
 */
public class FriendRequestControllerTest {

    private FriendRequestController controller;
    private FriendRequestService friendRequestService;

    @BeforeMethod
    public void setUp() {
        friendRequestService = mock(FriendRequestService.class);
        controller = new FriendRequestController(friendRequestService);
    }

    // ============================================================
    // SEND FRIEND REQUEST TESTS
    // ============================================================

    @Test
    public void sendFriendRequest_Success_Returns201() {
        // Given
        String userId = "user1";
        String username = "User One";
        String targetUserId = "user2";

        SendFriendRequestDto request = new SendFriendRequestDto(targetUserId);
        FriendRequestResponseDto expectedResponse = new FriendRequestResponseDto(
                "req123", userId, username, targetUserId, "PENDING", null, "Success");

        when(friendRequestService.sendFriendRequest(userId, username, targetUserId))
                .thenReturn(expectedResponse);

        // When
        ResponseEntity<FriendRequestResponseDto> response = controller.sendFriendRequest(request, userId, username);

        // Then
        Assert.assertEquals(response.getStatusCode(), HttpStatus.CREATED);
        Assert.assertNotNull(response.getBody());
        Assert.assertEquals(response.getBody().getId(), "req123");
    }

    @Test
    public void sendFriendRequest_NoUserId_Returns401() {
        // Given
        SendFriendRequestDto request = new SendFriendRequestDto("targetUser");

        // When
        ResponseEntity<FriendRequestResponseDto> response = controller.sendFriendRequest(request, null, null);

        // Then
        Assert.assertEquals(response.getStatusCode(), HttpStatus.UNAUTHORIZED);
        verify(friendRequestService, never()).sendFriendRequest(anyString(), anyString(), anyString());
    }

    @Test
    public void sendFriendRequest_NoUsername_FallsBackToUserId() {
        // Given
        String userId = "user1";
        String targetUserId = "user2";

        SendFriendRequestDto request = new SendFriendRequestDto(targetUserId);
        FriendRequestResponseDto expectedResponse = new FriendRequestResponseDto(
                "req123", userId, userId, targetUserId, "PENDING", null, "Success");

        // When username is null, userId is used as username
        when(friendRequestService.sendFriendRequest(userId, userId, targetUserId))
                .thenReturn(expectedResponse);

        // When
        ResponseEntity<FriendRequestResponseDto> response = controller.sendFriendRequest(request, userId, null);

        // Then
        Assert.assertEquals(response.getStatusCode(), HttpStatus.CREATED);
        verify(friendRequestService).sendFriendRequest(userId, userId, targetUserId);
    }

    @Test
    public void sendFriendRequest_SelfReferential_ServiceThrowsException() {
        // Given
        String userId = "user1";
        String username = "User One";
        SendFriendRequestDto request = new SendFriendRequestDto(userId);

        when(friendRequestService.sendFriendRequest(userId, username, userId))
                .thenThrow(new FriendRequestException(ErrorCode.SELF_REFERENTIAL_REQUEST));

        // When & Then
        try {
            controller.sendFriendRequest(request, userId, username);
            Assert.fail("Expected FriendRequestException");
        } catch (FriendRequestException e) {
            Assert.assertEquals(e.getErrorCode(), ErrorCode.SELF_REFERENTIAL_REQUEST);
        }
    }

    @Test
    public void sendFriendRequest_TargetNotFound_ServiceThrowsException() {
        // Given
        String userId = "user1";
        String username = "User One";
        String targetUserId = "nonexistent";
        SendFriendRequestDto request = new SendFriendRequestDto(targetUserId);

        when(friendRequestService.sendFriendRequest(userId, username, targetUserId))
                .thenThrow(new FriendRequestException(ErrorCode.USER_NOT_FOUND));

        // When & Then
        try {
            controller.sendFriendRequest(request, userId, username);
            Assert.fail("Expected FriendRequestException");
        } catch (FriendRequestException e) {
            Assert.assertEquals(e.getErrorCode(), ErrorCode.USER_NOT_FOUND);
        }
    }

    // ============================================================
    // ACCEPT FRIEND REQUEST TESTS
    // ============================================================

    @Test
    public void acceptFriendRequest_Success_Returns200() {
        // Given
        String userId = "user2";
        String username = "User Two";
        String requestId = "req123";

        AcceptFriendRequestDto request = new AcceptFriendRequestDto(requestId);
        FriendRequestResponseDto expectedResponse = new FriendRequestResponseDto(
                requestId, "user1", "User One", userId, "ACCEPTED", null, "Success");

        when(friendRequestService.acceptFriendRequest(userId, username, requestId))
                .thenReturn(expectedResponse);

        // When
        ResponseEntity<FriendRequestResponseDto> response = controller.acceptFriendRequest(request, userId, username);

        // Then
        Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
        Assert.assertNotNull(response.getBody());
        Assert.assertEquals(response.getBody().getStatus(), "ACCEPTED");
    }

    @Test
    public void acceptFriendRequest_NoUserId_Returns401() {
        // Given
        AcceptFriendRequestDto request = new AcceptFriendRequestDto("req123");

        // When
        ResponseEntity<FriendRequestResponseDto> response = controller.acceptFriendRequest(request, null, null);

        // Then
        Assert.assertEquals(response.getStatusCode(), HttpStatus.UNAUTHORIZED);
        verify(friendRequestService, never()).acceptFriendRequest(anyString(), anyString(), anyString());
    }

    @Test
    public void acceptFriendRequest_NoUsername_FallsBackToUserId() {
        // Given
        String userId = "user2";
        String requestId = "req123";

        AcceptFriendRequestDto request = new AcceptFriendRequestDto(requestId);
        FriendRequestResponseDto expectedResponse = new FriendRequestResponseDto(
                requestId, "user1", "User One", userId, "ACCEPTED", null, "Success");

        // When username is null, userId is used as username
        when(friendRequestService.acceptFriendRequest(userId, userId, requestId))
                .thenReturn(expectedResponse);

        // When
        ResponseEntity<FriendRequestResponseDto> response = controller.acceptFriendRequest(request, userId, null);

        // Then
        Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
        verify(friendRequestService).acceptFriendRequest(userId, userId, requestId);
    }

    @Test
    public void acceptFriendRequest_NotFound_ServiceThrowsException() {
        // Given
        String userId = "user2";
        String username = "User Two";
        String requestId = "nonexistent";
        AcceptFriendRequestDto request = new AcceptFriendRequestDto(requestId);

        when(friendRequestService.acceptFriendRequest(userId, username, requestId))
                .thenThrow(new FriendRequestException(ErrorCode.REQUEST_NOT_FOUND));

        // When & Then
        try {
            controller.acceptFriendRequest(request, userId, username);
            Assert.fail("Expected FriendRequestException");
        } catch (FriendRequestException e) {
            Assert.assertEquals(e.getErrorCode(), ErrorCode.REQUEST_NOT_FOUND);
        }
    }

    @Test
    public void acceptFriendRequest_AlreadyAccepted_ServiceThrowsException() {
        // Given
        String userId = "user2";
        String username = "User Two";
        String requestId = "req123";
        AcceptFriendRequestDto request = new AcceptFriendRequestDto(requestId);

        when(friendRequestService.acceptFriendRequest(userId, username, requestId))
                .thenThrow(new FriendRequestException(ErrorCode.REQUEST_ALREADY_ACCEPTED));

        // When & Then
        try {
            controller.acceptFriendRequest(request, userId, username);
            Assert.fail("Expected FriendRequestException");
        } catch (FriendRequestException e) {
            Assert.assertEquals(e.getErrorCode(), ErrorCode.REQUEST_ALREADY_ACCEPTED);
        }
    }

    // ============================================================
    // GET PENDING REQUESTS TESTS
    // ============================================================

    @Test
    public void getPendingRequests_Success_ReturnsList() {
        // Given
        String userId = "user1";

        FriendRequestResponseDto request1 = new FriendRequestResponseDto(
                "req1", "sender1", "Sender One", userId, "PENDING", null, null);
        FriendRequestResponseDto request2 = new FriendRequestResponseDto(
                "req2", "sender2", "Sender Two", userId, "PENDING", null, null);

        when(friendRequestService.getPendingRequests(userId))
                .thenReturn(List.of(request1, request2));

        // When
        ResponseEntity<List<FriendRequestResponseDto>> response = controller.getPendingRequests(userId);

        // Then
        Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
        Assert.assertNotNull(response.getBody());
        Assert.assertEquals(response.getBody().size(), 2);
    }

    @Test
    public void getPendingRequests_NoUserId_Returns401() {
        // When
        ResponseEntity<List<FriendRequestResponseDto>> response = controller.getPendingRequests(null);

        // Then
        Assert.assertEquals(response.getStatusCode(), HttpStatus.UNAUTHORIZED);
        verify(friendRequestService, never()).getPendingRequests(anyString());
    }

    @Test
    public void getPendingRequests_Empty_ReturnsEmptyList() {
        // Given
        String userId = "user1";

        when(friendRequestService.getPendingRequests(userId))
                .thenReturn(List.of());

        // When
        ResponseEntity<List<FriendRequestResponseDto>> response = controller.getPendingRequests(userId);

        // Then
        Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
        Assert.assertNotNull(response.getBody());
        Assert.assertTrue(response.getBody().isEmpty());
    }
}
