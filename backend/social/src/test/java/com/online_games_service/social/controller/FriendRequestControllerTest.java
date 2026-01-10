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

import java.security.Principal;
import java.util.List;

import static org.mockito.Mockito.*;

/**
 * Unit tests for FriendRequestController.
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
        String currentUserId = "user1";
        String targetUserId = "user2";
        Principal principal = () -> currentUserId;

        SendFriendRequestDto request = new SendFriendRequestDto(targetUserId);
        FriendRequestResponseDto expectedResponse = new FriendRequestResponseDto(
                "req123", currentUserId, targetUserId, "PENDING", "Success");

        when(friendRequestService.sendFriendRequest(currentUserId, currentUserId, targetUserId))
                .thenReturn(expectedResponse);

        // When
        ResponseEntity<FriendRequestResponseDto> response = controller.sendFriendRequest(request, principal);

        // Then
        Assert.assertEquals(response.getStatusCode(), HttpStatus.CREATED);
        Assert.assertNotNull(response.getBody());
        Assert.assertEquals(response.getBody().getRequestId(), "req123");
    }

    @Test
    public void sendFriendRequest_NoPrincipal_Returns401() {
        // Given
        SendFriendRequestDto request = new SendFriendRequestDto("targetUser");

        // When
        ResponseEntity<FriendRequestResponseDto> response = controller.sendFriendRequest(request, null);

        // Then
        Assert.assertEquals(response.getStatusCode(), HttpStatus.UNAUTHORIZED);
        verify(friendRequestService, never()).sendFriendRequest(anyString(), anyString(), anyString());
    }

    @Test
    public void sendFriendRequest_SelfReferential_ServiceThrowsException() {
        // Given
        String userId = "user1";
        Principal principal = () -> userId;
        SendFriendRequestDto request = new SendFriendRequestDto(userId);

        when(friendRequestService.sendFriendRequest(userId, userId, userId))
                .thenThrow(new FriendRequestException(ErrorCode.SELF_REFERENTIAL_REQUEST));

        // When & Then
        try {
            controller.sendFriendRequest(request, principal);
            Assert.fail("Expected FriendRequestException");
        } catch (FriendRequestException e) {
            Assert.assertEquals(e.getErrorCode(), ErrorCode.SELF_REFERENTIAL_REQUEST);
        }
    }

    @Test
    public void sendFriendRequest_TargetNotFound_ServiceThrowsException() {
        // Given
        String currentUserId = "user1";
        String targetUserId = "nonexistent";
        Principal principal = () -> currentUserId;
        SendFriendRequestDto request = new SendFriendRequestDto(targetUserId);

        when(friendRequestService.sendFriendRequest(currentUserId, currentUserId, targetUserId))
                .thenThrow(new FriendRequestException(ErrorCode.USER_NOT_FOUND));

        // When & Then
        try {
            controller.sendFriendRequest(request, principal);
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
        String currentUserId = "user2";
        String requestId = "req123";
        Principal principal = () -> currentUserId;

        AcceptFriendRequestDto request = new AcceptFriendRequestDto(requestId);
        FriendRequestResponseDto expectedResponse = new FriendRequestResponseDto(
                requestId, "user1", currentUserId, "ACCEPTED", "Success");

        when(friendRequestService.acceptFriendRequest(currentUserId, currentUserId, requestId))
                .thenReturn(expectedResponse);

        // When
        ResponseEntity<FriendRequestResponseDto> response = controller.acceptFriendRequest(request, principal);

        // Then
        Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
        Assert.assertNotNull(response.getBody());
        Assert.assertEquals(response.getBody().getStatus(), "ACCEPTED");
    }

    @Test
    public void acceptFriendRequest_NoPrincipal_Returns401() {
        // Given
        AcceptFriendRequestDto request = new AcceptFriendRequestDto("req123");

        // When
        ResponseEntity<FriendRequestResponseDto> response = controller.acceptFriendRequest(request, null);

        // Then
        Assert.assertEquals(response.getStatusCode(), HttpStatus.UNAUTHORIZED);
        verify(friendRequestService, never()).acceptFriendRequest(anyString(), anyString(), anyString());
    }

    @Test
    public void acceptFriendRequest_NotFound_ServiceThrowsException() {
        // Given
        String currentUserId = "user2";
        String requestId = "nonexistent";
        Principal principal = () -> currentUserId;
        AcceptFriendRequestDto request = new AcceptFriendRequestDto(requestId);

        when(friendRequestService.acceptFriendRequest(currentUserId, currentUserId, requestId))
                .thenThrow(new FriendRequestException(ErrorCode.REQUEST_NOT_FOUND));

        // When & Then
        try {
            controller.acceptFriendRequest(request, principal);
            Assert.fail("Expected FriendRequestException");
        } catch (FriendRequestException e) {
            Assert.assertEquals(e.getErrorCode(), ErrorCode.REQUEST_NOT_FOUND);
        }
    }

    @Test
    public void acceptFriendRequest_AlreadyAccepted_ServiceThrowsException() {
        // Given
        String currentUserId = "user2";
        String requestId = "req123";
        Principal principal = () -> currentUserId;
        AcceptFriendRequestDto request = new AcceptFriendRequestDto(requestId);

        when(friendRequestService.acceptFriendRequest(currentUserId, currentUserId, requestId))
                .thenThrow(new FriendRequestException(ErrorCode.REQUEST_ALREADY_ACCEPTED));

        // When & Then
        try {
            controller.acceptFriendRequest(request, principal);
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
        String currentUserId = "user1";
        Principal principal = () -> currentUserId;

        FriendRequest request1 = new FriendRequest("sender1", currentUserId);
        request1.setId("req1");
        FriendRequest request2 = new FriendRequest("sender2", currentUserId);
        request2.setId("req2");

        when(friendRequestService.getPendingRequests(currentUserId))
                .thenReturn(List.of(request1, request2));

        // When
        ResponseEntity<List<FriendRequest>> response = controller.getPendingRequests(principal);

        // Then
        Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
        Assert.assertNotNull(response.getBody());
        Assert.assertEquals(response.getBody().size(), 2);
    }

    @Test
    public void getPendingRequests_NoPrincipal_Returns401() {
        // When
        ResponseEntity<List<FriendRequest>> response = controller.getPendingRequests(null);

        // Then
        Assert.assertEquals(response.getStatusCode(), HttpStatus.UNAUTHORIZED);
        verify(friendRequestService, never()).getPendingRequests(anyString());
    }

    @Test
    public void getPendingRequests_Empty_ReturnsEmptyList() {
        // Given
        String currentUserId = "user1";
        Principal principal = () -> currentUserId;

        when(friendRequestService.getPendingRequests(currentUserId))
                .thenReturn(List.of());

        // When
        ResponseEntity<List<FriendRequest>> response = controller.getPendingRequests(principal);

        // Then
        Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
        Assert.assertNotNull(response.getBody());
        Assert.assertTrue(response.getBody().isEmpty());
    }
}
