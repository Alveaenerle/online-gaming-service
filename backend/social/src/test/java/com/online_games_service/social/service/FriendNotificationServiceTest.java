package com.online_games_service.social.service;

import com.online_games_service.social.dto.PresenceUpdateMessage;
import com.online_games_service.social.model.SocialProfile;
import com.online_games_service.social.repository.SocialProfileRepository;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Optional;
import java.util.Set;

import static org.mockito.Mockito.*;

public class FriendNotificationServiceTest {

    private FriendNotificationService friendNotificationService;
    private SocialProfileRepository socialProfileRepository;
    private SimpMessagingTemplate messagingTemplate;
    private PresenceService presenceService;

    @BeforeMethod
    public void setUp() {
        socialProfileRepository = mock(SocialProfileRepository.class);
        messagingTemplate = mock(SimpMessagingTemplate.class);
        presenceService = mock(PresenceService.class);
        
        friendNotificationService = new FriendNotificationService(
                socialProfileRepository,
                messagingTemplate,
                presenceService
        );
    }

    @Test
    public void shouldNotifyOnlineFriendsWhenUserGoesOnline() {
        // Given
        String userId = "user123";
        String friend1 = "friend1";
        String friend2 = "friend2";
        String friend3 = "friend3"; // This one is offline
        
        SocialProfile profile = new SocialProfile(userId);
        profile.setFriendIds(Set.of(friend1, friend2, friend3));
        
        when(socialProfileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(presenceService.isUserOnline(friend1)).thenReturn(true);
        when(presenceService.isUserOnline(friend2)).thenReturn(true);
        when(presenceService.isUserOnline(friend3)).thenReturn(false);

        // When
        friendNotificationService.notifyFriendsUserOnline(userId);

        // Then
        verify(messagingTemplate).convertAndSendToUser(
                eq(friend1),
                eq("/queue/presence"),
                argThat(msg -> {
                    PresenceUpdateMessage m = (PresenceUpdateMessage) msg;
                    return m.getUserId().equals(userId) && 
                           m.getStatus() == PresenceUpdateMessage.PresenceStatus.ONLINE;
                })
        );
        
        verify(messagingTemplate).convertAndSendToUser(
                eq(friend2),
                eq("/queue/presence"),
                any(PresenceUpdateMessage.class)
        );
        
        // friend3 is offline, should not receive notification
        verify(messagingTemplate, never()).convertAndSendToUser(
                eq(friend3),
                anyString(),
                any()
        );
    }

    @Test
    public void shouldNotifyOnlineFriendsWhenUserGoesOffline() {
        // Given
        String userId = "user456";
        String friend1 = "friend1";
        
        SocialProfile profile = new SocialProfile(userId);
        profile.setFriendIds(Set.of(friend1));
        
        when(socialProfileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(presenceService.isUserOnline(friend1)).thenReturn(true);

        // When
        friendNotificationService.notifyFriendsUserOffline(userId);

        // Then
        ArgumentCaptor<PresenceUpdateMessage> messageCaptor = 
                ArgumentCaptor.forClass(PresenceUpdateMessage.class);
        
        verify(messagingTemplate).convertAndSendToUser(
                eq(friend1),
                eq("/queue/presence"),
                messageCaptor.capture()
        );
        
        PresenceUpdateMessage sentMessage = messageCaptor.getValue();
        Assert.assertEquals(sentMessage.getUserId(), userId);
        Assert.assertEquals(sentMessage.getStatus(), PresenceUpdateMessage.PresenceStatus.OFFLINE);
    }

    @Test
    public void shouldNotSendNotificationWhenUserHasNoFriends() {
        // Given
        String userId = "lonely_user";
        
        SocialProfile profile = new SocialProfile(userId);
        // No friends added
        
        when(socialProfileRepository.findById(userId)).thenReturn(Optional.of(profile));

        // When
        friendNotificationService.notifyFriendsUserOnline(userId);

        // Then
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    public void shouldHandleUserWithNoProfile() {
        // Given
        String userId = "no_profile_user";
        
        when(socialProfileRepository.findById(userId)).thenReturn(Optional.empty());

        // When
        friendNotificationService.notifyFriendsUserOnline(userId);

        // Then
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    public void shouldHandleMessagingException() {
        // Given
        String userId = "user_with_error";
        String friend1 = "friend_error";
        
        SocialProfile profile = new SocialProfile(userId);
        profile.setFriendIds(Set.of(friend1));
        
        when(socialProfileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(presenceService.isUserOnline(friend1)).thenReturn(true);
        doThrow(new RuntimeException("Connection error")).when(messagingTemplate)
                .convertAndSendToUser(anyString(), anyString(), any());

        // When - should not throw exception
        friendNotificationService.notifyFriendsUserOnline(userId);

        // Then - method should complete without throwing
        verify(messagingTemplate).convertAndSendToUser(eq(friend1), anyString(), any());
    }

    @Test
    public void shouldOnlyNotifyOnlineFriends() {
        // Given
        String userId = "user789";
        
        SocialProfile profile = new SocialProfile(userId);
        profile.setFriendIds(Set.of("offline_friend_1", "offline_friend_2"));
        
        when(socialProfileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(presenceService.isUserOnline(anyString())).thenReturn(false);

        // When
        friendNotificationService.notifyFriendsUserOnline(userId);

        // Then
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    public void shouldSendFriendRequestNotification() {
        // Given
        String targetUserId = "targetUser";
        String fromUserId = "user1";
        String fromUserName = "User One";

        // When
        friendNotificationService.sendFriendRequestNotification(targetUserId, fromUserId, fromUserName);

        // Then
        verify(messagingTemplate).convertAndSendToUser(
                eq(targetUserId),
                eq("/queue/notifications"),
                argThat((Object notification) -> {
                    if (notification instanceof java.util.Map) {
                        java.util.Map map = (java.util.Map) notification;
                        return "FRIEND_REQUEST".equals(map.get("subType")) &&
                               fromUserId.equals(map.get("senderId")) &&
                               fromUserName.equals(map.get("senderName"));
                    }
                    return false;
                })
        );
    }

    @Test
    public void shouldHandleErrorWhenSendingFriendRequestNotification() {
        // Given
        String targetUserId = "targetUser";
        doThrow(new RuntimeException("Error")).when(messagingTemplate)
                .convertAndSendToUser(anyString(), anyString(), any());

        // When
        friendNotificationService.sendFriendRequestNotification(targetUserId, "user1", "User One");

        // Then
        verify(messagingTemplate).convertAndSendToUser(eq(targetUserId), anyString(), any());
    }

    @Test
    public void shouldSendRequestAcceptedNotification() {
        // Given
        String targetUserId = "targetUser";
        String accepterId = "user2";
        String accepterName = "User Two";

        // When
        friendNotificationService.sendRequestAcceptedNotification(targetUserId, accepterId, accepterName);

        // Then
        verify(messagingTemplate).convertAndSendToUser(
                eq(targetUserId),
                eq("/queue/notifications"),
                argThat((Object notification) -> {
                    if (notification instanceof java.util.Map) {
                        java.util.Map map = (java.util.Map) notification;
                        return "REQUEST_ACCEPTED".equals(map.get("subType")) &&
                               accepterId.equals(map.get("accepterId")) &&
                               accepterName.equals(map.get("accepterName"));
                    }
                    return false;
                })
        );
    }

    @Test
    public void shouldHandleErrorWhenSendingRequestAcceptedNotification() {
        // Given
        String targetUserId = "targetUser";
        doThrow(new RuntimeException("Error")).when(messagingTemplate)
                .convertAndSendToUser(anyString(), anyString(), any());

        // When
        friendNotificationService.sendRequestAcceptedNotification(targetUserId, "user2", "User Two");

        // Then
        verify(messagingTemplate).convertAndSendToUser(eq(targetUserId), anyString(), any());
    }

    @Test
    public void shouldSendMutualPresenceUpdatesWhenBothUsersOnline() {
        // Given
        String user1 = "user1";
        String user2 = "user2";
        
        when(presenceService.isUserOnline(user1)).thenReturn(true);
        when(presenceService.isUserOnline(user2)).thenReturn(true);

        // When
        friendNotificationService.sendMutualPresenceUpdates(user1, user2);

        // Then - user1 receives user2's status
        verify(messagingTemplate).convertAndSendToUser(
                eq(user1),
                eq("/queue/presence"),
                argThat(msg -> {
                    PresenceUpdateMessage m = (PresenceUpdateMessage) msg;
                    return m.getUserId().equals(user2) && 
                           m.getStatus() == PresenceUpdateMessage.PresenceStatus.ONLINE;
                })
        );
        
        // user2 receives user1's status
        verify(messagingTemplate).convertAndSendToUser(
                eq(user2),
                eq("/queue/presence"),
                argThat(msg -> {
                    PresenceUpdateMessage m = (PresenceUpdateMessage) msg;
                    return m.getUserId().equals(user1) && 
                           m.getStatus() == PresenceUpdateMessage.PresenceStatus.ONLINE;
                })
        );
    }

    @Test
    public void shouldSendMutualPresenceUpdatesWhenOnlyOneUserOnline() {
        // Given
        String user1 = "user1";
        String user2 = "user2";
        
        when(presenceService.isUserOnline(user1)).thenReturn(true);
        when(presenceService.isUserOnline(user2)).thenReturn(false);

        // When
        friendNotificationService.sendMutualPresenceUpdates(user1, user2);

        // Then - user1 (online) receives user2's OFFLINE status
        verify(messagingTemplate).convertAndSendToUser(
                eq(user1),
                eq("/queue/presence"),
                argThat(msg -> {
                    PresenceUpdateMessage m = (PresenceUpdateMessage) msg;
                    return m.getUserId().equals(user2) && 
                           m.getStatus() == PresenceUpdateMessage.PresenceStatus.OFFLINE;
                })
        );
        
        // user2 (offline) should NOT receive any notification
        verify(messagingTemplate, never()).convertAndSendToUser(
                eq(user2),
                anyString(),
                any()
        );
    }

    @Test
    public void shouldNotSendPresenceUpdatesWhenBothUsersOffline() {
        // Given
        String user1 = "user1";
        String user2 = "user2";
        
        when(presenceService.isUserOnline(user1)).thenReturn(false);
        when(presenceService.isUserOnline(user2)).thenReturn(false);

        // When
        friendNotificationService.sendMutualPresenceUpdates(user1, user2);

        // Then - no notifications sent
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    public void shouldHandleErrorInMutualPresenceUpdates() {
        // Given
        String user1 = "user1";
        String user2 = "user2";
        
        when(presenceService.isUserOnline(user1)).thenReturn(true);
        when(presenceService.isUserOnline(user2)).thenReturn(true);
        
        doThrow(new RuntimeException("Error")).when(messagingTemplate)
                .convertAndSendToUser(eq(user1), anyString(), any());

        // When
        friendNotificationService.sendMutualPresenceUpdates(user1, user2);

        // Then - should still try to send to user2 despite error for user1
        verify(messagingTemplate).convertAndSendToUser(eq(user1), anyString(), any());
        verify(messagingTemplate).convertAndSendToUser(eq(user2), anyString(), any());
    }

    // ============================================================
    // GAME INVITE NOTIFICATION TESTS
    // ============================================================

    @Test
    @SuppressWarnings("unchecked")
    public void shouldSendGameInviteNotification() {
        // Given
        String targetUserId = "target1";
        String inviteId = "invite123";
        String senderId = "sender1";
        String senderUsername = "Alice";
        String lobbyId = "lobby1";
        String lobbyName = "Fun Game";
        String gameType = "MAKAO";
        String accessCode = "ABC123";

        // When
        friendNotificationService.sendGameInviteNotification(
                targetUserId, inviteId, senderId, senderUsername, 
                lobbyId, lobbyName, gameType, accessCode);

        // Then
        ArgumentCaptor<java.util.Map<String, Object>> messageCaptor = ArgumentCaptor.forClass(java.util.Map.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq(targetUserId), 
                eq("/queue/notifications"), 
                messageCaptor.capture()
        );

        java.util.Map<String, Object> notification = messageCaptor.getValue();
        Assert.assertEquals(notification.get("type"), "NOTIFICATION_RECEIVED");
        Assert.assertEquals(notification.get("subType"), "GAME_INVITE");

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> gameInvite = (java.util.Map<String, Object>) notification.get("gameInvite");
        Assert.assertEquals(gameInvite.get("id"), inviteId);
        Assert.assertEquals(gameInvite.get("senderId"), senderId);
        Assert.assertEquals(gameInvite.get("senderUsername"), senderUsername);
        Assert.assertEquals(gameInvite.get("lobbyId"), lobbyId);
        Assert.assertEquals(gameInvite.get("lobbyName"), lobbyName);
        Assert.assertEquals(gameInvite.get("gameType"), gameType);
        Assert.assertEquals(gameInvite.get("accessCode"), accessCode);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldSendGameInviteNotificationWithNullAccessCode() {
        // Given
        String targetUserId = "target1";
        String inviteId = "invite123";

        // When
        friendNotificationService.sendGameInviteNotification(
                targetUserId, inviteId, "sender", "Sender", 
                "lobby", "Lobby", "LUDO", null);

        // Then
        ArgumentCaptor<java.util.Map<String, Object>> messageCaptor = ArgumentCaptor.forClass(java.util.Map.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq(targetUserId), 
                eq("/queue/notifications"), 
                messageCaptor.capture()
        );

        java.util.Map<String, Object> notification = messageCaptor.getValue();
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> gameInvite = (java.util.Map<String, Object>) notification.get("gameInvite");
        Assert.assertEquals(gameInvite.get("accessCode"), "");
    }

    @Test
    public void shouldHandleExceptionInGameInviteNotification() {
        // Given
        doThrow(new RuntimeException("Error")).when(messagingTemplate)
                .convertAndSendToUser(anyString(), anyString(), any());

        // When - should not throw
        friendNotificationService.sendGameInviteNotification(
                "target", "invite", "sender", "Sender", 
                "lobby", "Lobby", "MAKAO", "CODE");

        // Then
        verify(messagingTemplate).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldSendFriendRemovedNotification() {
        // Given
        String removedUserId = "removedUser123";
        String removedByUserId = "removerUser456";

        // When
        friendNotificationService.sendFriendRemovedNotification(removedUserId, removedByUserId);

        // Then
        ArgumentCaptor<java.util.Map<String, Object>> messageCaptor = ArgumentCaptor.forClass(java.util.Map.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq(removedUserId),
                eq("/queue/notifications"),
                messageCaptor.capture()
        );

        java.util.Map<String, Object> notification = messageCaptor.getValue();
        Assert.assertEquals(notification.get("type"), "NOTIFICATION_RECEIVED");
        Assert.assertEquals(notification.get("subType"), "FRIEND_REMOVED");
        Assert.assertEquals(notification.get("removedByUserId"), removedByUserId);
    }

    @Test
    public void shouldHandleExceptionInFriendRemovedNotification() {
        // Given
        doThrow(new RuntimeException("Error")).when(messagingTemplate)
                .convertAndSendToUser(anyString(), anyString(), any());

        // When - should not throw
        friendNotificationService.sendFriendRemovedNotification("target", "remover");

        // Then
        verify(messagingTemplate).convertAndSendToUser(anyString(), anyString(), any());
    }
}
