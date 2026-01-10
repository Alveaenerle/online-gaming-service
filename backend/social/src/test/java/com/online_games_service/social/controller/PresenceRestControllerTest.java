package com.online_games_service.social.controller;

import com.online_games_service.social.dto.FriendsStatusRequest;
import com.online_games_service.social.dto.FriendsStatusResponse;
import com.online_games_service.social.dto.UserPresenceStatus;
import com.online_games_service.social.service.PresenceService;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
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

    @Test
    public void shouldGetFriendsStatus() {
        // Given
        List<String> friendIds = Arrays.asList("friend1", "friend2");
        FriendsStatusRequest request = new FriendsStatusRequest(friendIds);
        
        List<UserPresenceStatus> mockStatuses = Arrays.asList(
                new UserPresenceStatus("friend1", true),
                new UserPresenceStatus("friend2", false)
        );
        when(presenceService.getUsersOnlineStatus(friendIds)).thenReturn(mockStatuses);

        // When
        ResponseEntity<FriendsStatusResponse> response = restController.getFriendsStatus(request);

        // Then
        Assert.assertNotNull(response.getBody());
        Assert.assertEquals(response.getBody().getFriends().size(), 2);
        Assert.assertTrue(response.getBody().getFriends().get(0).isOnline());
        Assert.assertFalse(response.getBody().getFriends().get(1).isOnline());
    }

    @Test
    public void shouldGetSingleUserPresence() {
        // Given
        String userId = "single_user";
        when(presenceService.isUserOnline(userId)).thenReturn(true);

        // When
        ResponseEntity<UserPresenceStatus> response = restController.getUserPresence(userId);

        // Then
        Assert.assertNotNull(response.getBody());
        Assert.assertEquals(response.getBody().getUserId(), userId);
        Assert.assertTrue(response.getBody().isOnline());
    }

    @Test
    public void shouldReturnOfflineForNonExistentUser() {
        // Given
        String userId = "offline_user";
        when(presenceService.isUserOnline(userId)).thenReturn(false);

        // When
        ResponseEntity<UserPresenceStatus> response = restController.getUserPresence(userId);

        // Then
        Assert.assertNotNull(response.getBody());
        Assert.assertEquals(response.getBody().getUserId(), userId);
        Assert.assertFalse(response.getBody().isOnline());
    }

    @Test
    public void shouldHandleEmptyFriendsList() {
        // Given
        FriendsStatusRequest request = new FriendsStatusRequest(List.of());
        when(presenceService.getUsersOnlineStatus(List.of())).thenReturn(List.of());

        // When
        ResponseEntity<FriendsStatusResponse> response = restController.getFriendsStatus(request);

        // Then
        Assert.assertNotNull(response.getBody());
        Assert.assertTrue(response.getBody().getFriends().isEmpty());
    }
}
