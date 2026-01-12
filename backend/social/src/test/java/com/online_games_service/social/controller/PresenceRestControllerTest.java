package com.online_games_service.social.controller;

import com.online_games_service.social.dto.FriendsStatusRequest;
import com.online_games_service.social.dto.FriendsStatusResponse;
import com.online_games_service.social.dto.UserPresenceStatus;
import com.online_games_service.social.service.PresenceService;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

public class PresenceRestControllerTest {

    private PresenceRestController restController;
    private PresenceService presenceService;

    @BeforeMethod
    public void setUp() {
        presenceService = mock(PresenceService.class);
        restController = new PresenceRestController(presenceService);
    }

    // ============================================================
    // GET FRIENDS STATUS TESTS
    // ============================================================

    @Test
    public void getFriendsStatus_Success_ReturnsStatuses() {
        // Given
        String userId = "authenticated_user";
        List<String> friendIds = Arrays.asList("friend1", "friend2");
        FriendsStatusRequest request = new FriendsStatusRequest(friendIds);
        
        List<UserPresenceStatus> mockStatuses = Arrays.asList(
                new UserPresenceStatus("friend1", true),
                new UserPresenceStatus("friend2", false)
        );
        when(presenceService.getUsersOnlineStatus(friendIds)).thenReturn(mockStatuses);

        // When
        ResponseEntity<FriendsStatusResponse> response = restController.getFriendsStatus(request, userId);

        // Then
        Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
        Assert.assertNotNull(response.getBody());
        Assert.assertEquals(response.getBody().getFriends().size(), 2);
        Assert.assertTrue(response.getBody().getFriends().get(0).isOnline());
        Assert.assertFalse(response.getBody().getFriends().get(1).isOnline());
    }

    @Test
    public void getFriendsStatus_NoUserId_Returns401() {
        // Given
        FriendsStatusRequest request = new FriendsStatusRequest(List.of("friend1"));

        // When
        ResponseEntity<FriendsStatusResponse> response = restController.getFriendsStatus(request, null);

        // Then
        Assert.assertEquals(response.getStatusCode(), HttpStatus.UNAUTHORIZED);
        verify(presenceService, never()).getUsersOnlineStatus(anyList());
    }

    @Test
    public void getFriendsStatus_EmptyList_ReturnsEmptyResponse() {
        // Given
        String userId = "authenticated_user";
        FriendsStatusRequest request = new FriendsStatusRequest(List.of());
        when(presenceService.getUsersOnlineStatus(List.of())).thenReturn(List.of());

        // When
        ResponseEntity<FriendsStatusResponse> response = restController.getFriendsStatus(request, userId);

        // Then
        Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
        Assert.assertNotNull(response.getBody());
        Assert.assertTrue(response.getBody().getFriends().isEmpty());
    }

    // ============================================================
    // GET USER PRESENCE TESTS
    // ============================================================

    @Test
    public void getUserPresence_UserOnline_ReturnsOnlineStatus() {
        // Given
        String userId = "authenticated_user";
        String targetUserId = "single_user";
        when(presenceService.isUserOnline(targetUserId)).thenReturn(true);

        // When
        ResponseEntity<UserPresenceStatus> response = restController.getUserPresence(targetUserId, userId);

        // Then
        Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
        Assert.assertNotNull(response.getBody());
        Assert.assertEquals(response.getBody().getUserId(), targetUserId);
        Assert.assertTrue(response.getBody().isOnline());
    }

    @Test
    public void getUserPresence_UserOffline_ReturnsOfflineStatus() {
        // Given
        String userId = "authenticated_user";
        String targetUserId = "offline_user";
        when(presenceService.isUserOnline(targetUserId)).thenReturn(false);

        // When
        ResponseEntity<UserPresenceStatus> response = restController.getUserPresence(targetUserId, userId);

        // Then
        Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
        Assert.assertNotNull(response.getBody());
        Assert.assertEquals(response.getBody().getUserId(), targetUserId);
        Assert.assertFalse(response.getBody().isOnline());
    }

    @Test
    public void getUserPresence_NoUserId_Returns401() {
        // When
        ResponseEntity<UserPresenceStatus> response = restController.getUserPresence("target", null);

        // Then
        Assert.assertEquals(response.getStatusCode(), HttpStatus.UNAUTHORIZED);
        verify(presenceService, never()).isUserOnline(anyString());
    }
}
