package com.online_games_service.social.controller;

import com.online_games_service.social.dto.FriendsStatusRequest;
import com.online_games_service.social.dto.FriendsStatusResponse;
import com.online_games_service.social.dto.UserPresenceStatus;
import com.online_games_service.social.service.FriendNotificationService;
import com.online_games_service.social.service.PresenceService;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

public class PresenceControllerTest {

    private PresenceController presenceController;
    private PresenceService presenceService;
    private FriendNotificationService friendNotificationService;

    @BeforeMethod
    public void setUp() {
        presenceService = mock(PresenceService.class);
        friendNotificationService = mock(FriendNotificationService.class);
        presenceController = new PresenceController(presenceService, friendNotificationService);
    }

    @Test
    public void shouldHandlePingAndRefreshPresence() {
        // Given
        String userId = "pinging_user";
        Principal principal = () -> userId;
        when(presenceService.isUserOnline(userId)).thenReturn(true);

        // When
        presenceController.handlePing(principal);

        // Then
        verify(presenceService).isUserOnline(userId);
        verify(presenceService).refreshUserPresence(userId);
        // User was already online, so no notification should be sent
        verify(friendNotificationService, never()).notifyFriendsUserOnline(userId);
    }

    @Test
    public void shouldNotifyFriendsOnFirstPing() {
        // Given
        String userId = "new_pinging_user";
        Principal principal = () -> userId;
        when(presenceService.isUserOnline(userId)).thenReturn(false);

        // When
        presenceController.handlePing(principal);

        // Then
        verify(presenceService).refreshUserPresence(userId);
        verify(friendNotificationService).notifyFriendsUserOnline(userId);
    }

    @Test
    public void shouldIgnorePingWithoutPrincipal() {
        // When
        presenceController.handlePing(null);

        // Then
        verify(presenceService, never()).refreshUserPresence(anyString());
    }

    @Test
    public void shouldGetFriendsStatus() {
        // Given
        String userId = "requesting_user";
        Principal principal = () -> userId;
        
        List<String> friendIds = Arrays.asList("friend1", "friend2", "friend3");
        FriendsStatusRequest request = new FriendsStatusRequest(friendIds);
        
        List<UserPresenceStatus> mockStatuses = Arrays.asList(
                new UserPresenceStatus("friend1", true),
                new UserPresenceStatus("friend2", false),
                new UserPresenceStatus("friend3", true)
        );
        when(presenceService.getUsersOnlineStatus(friendIds)).thenReturn(mockStatuses);

        // When
        FriendsStatusResponse response = presenceController.getFriendsStatus(request, principal);

        // Then
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getFriends().size(), 3);
        Assert.assertTrue(response.getFriends().get(0).isOnline());
        Assert.assertFalse(response.getFriends().get(1).isOnline());
    }

    @Test
    public void shouldReturnEmptyListWithoutPrincipal() {
        // Given
        FriendsStatusRequest request = new FriendsStatusRequest(Arrays.asList("friend1"));

        // When
        FriendsStatusResponse response = presenceController.getFriendsStatus(request, null);

        // Then
        Assert.assertNotNull(response);
        Assert.assertTrue(response.getFriends().isEmpty());
        verify(presenceService, never()).getUsersOnlineStatus(any());
    }

    @Test
    public void shouldHandleLogout() {
        // Given
        String userId = "logging_out_user";
        Principal principal = () -> userId;

        // When
        presenceController.handleLogout(principal);

        // Then
        verify(presenceService).removeUserOnline(userId);
        verify(friendNotificationService).notifyFriendsUserOffline(userId);
    }

    @Test
    public void shouldIgnoreLogoutWithoutPrincipal() {
        // When
        presenceController.handleLogout(null);

        // Then
        verify(presenceService, never()).removeUserOnline(anyString());
        verify(friendNotificationService, never()).notifyFriendsUserOffline(anyString());
    }
}
