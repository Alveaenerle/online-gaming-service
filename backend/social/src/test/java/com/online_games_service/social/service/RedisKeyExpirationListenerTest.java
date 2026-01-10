package com.online_games_service.social.service;

import com.online_games_service.social.config.SocialRedisConfig;
import org.springframework.data.redis.connection.Message;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;

public class RedisKeyExpirationListenerTest {

    private RedisKeyExpirationListener listener;
    private PresenceService presenceService;
    private FriendNotificationService friendNotificationService;

    @BeforeMethod
    public void setUp() {
        presenceService = mock(PresenceService.class);
        friendNotificationService = mock(FriendNotificationService.class);
        listener = new RedisKeyExpirationListener(presenceService, friendNotificationService);
    }

    @Test
    public void shouldHandlePresenceKeyExpiration() {
        // Given
        String userId = "expired_user";
        String key = SocialRedisConfig.ONLINE_USER_KEY_PREFIX + userId;
        
        Message message = mock(Message.class);
        when(message.getBody()).thenReturn(key.getBytes());
        when(presenceService.extractUserIdFromKey(key)).thenReturn(userId);

        // When
        listener.onMessage(message, null);

        // Then
        verify(presenceService).extractUserIdFromKey(key);
        verify(friendNotificationService).notifyFriendsUserOffline(userId);
    }

    @Test
    public void shouldIgnoreNonPresenceKeys() {
        // Given
        String key = "some:other:key:pattern";
        
        Message message = mock(Message.class);
        when(message.getBody()).thenReturn(key.getBytes());

        // When
        listener.onMessage(message, null);

        // Then
        verify(friendNotificationService, never()).notifyFriendsUserOffline(anyString());
    }

    @Test
    public void shouldHandleNullUserIdFromKeyExtraction() {
        // Given
        String key = SocialRedisConfig.ONLINE_USER_KEY_PREFIX; // Invalid, no userId
        
        Message message = mock(Message.class);
        when(message.getBody()).thenReturn(key.getBytes());
        when(presenceService.extractUserIdFromKey(key)).thenReturn(null);

        // When
        listener.onMessage(message, null);

        // Then
        verify(friendNotificationService, never()).notifyFriendsUserOffline(anyString());
    }

    @Test
    public void shouldHandleExceptionInNotification() {
        // Given
        String userId = "error_user";
        String key = SocialRedisConfig.ONLINE_USER_KEY_PREFIX + userId;
        
        Message message = mock(Message.class);
        when(message.getBody()).thenReturn(key.getBytes());
        when(presenceService.extractUserIdFromKey(key)).thenReturn(userId);
        doThrow(new RuntimeException("Notification error")).when(friendNotificationService)
                .notifyFriendsUserOffline(userId);

        // When - should not throw
        listener.onMessage(message, null);

        // Then - exception is caught and logged
        verify(friendNotificationService).notifyFriendsUserOffline(userId);
    }

    @Test
    public void shouldHandleMultipleExpirations() {
        // Given
        String userId1 = "user1";
        String userId2 = "user2";
        String key1 = SocialRedisConfig.ONLINE_USER_KEY_PREFIX + userId1;
        String key2 = SocialRedisConfig.ONLINE_USER_KEY_PREFIX + userId2;
        
        Message message1 = mock(Message.class);
        when(message1.getBody()).thenReturn(key1.getBytes());
        when(presenceService.extractUserIdFromKey(key1)).thenReturn(userId1);
        
        Message message2 = mock(Message.class);
        when(message2.getBody()).thenReturn(key2.getBytes());
        when(presenceService.extractUserIdFromKey(key2)).thenReturn(userId2);

        // When
        listener.onMessage(message1, null);
        listener.onMessage(message2, null);

        // Then
        verify(friendNotificationService).notifyFriendsUserOffline(userId1);
        verify(friendNotificationService).notifyFriendsUserOffline(userId2);
    }
}
