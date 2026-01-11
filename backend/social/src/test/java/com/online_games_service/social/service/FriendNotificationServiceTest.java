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
}
