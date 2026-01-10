package com.online_games_service.social.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.online_games_service.social.dto.NotificationPayload;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;

/**
 * Unit tests for RedisNotificationPublisher.
 */
public class RedisNotificationPublisherTest {

    private RedisNotificationPublisher publisher;
    private StringRedisTemplate redisTemplate;
    private ObjectMapper objectMapper;

    @BeforeMethod
    public void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        objectMapper = new ObjectMapper();
        publisher = new RedisNotificationPublisher(redisTemplate, objectMapper);
    }

    @Test
    public void publishFriendRequest_PublishesToCorrectChannel() {
        // Given
        String targetUserId = "user123";
        String fromUserId = "sender456";
        String fromUserName = "Alice";

        // When
        publisher.publishFriendRequest(targetUserId, fromUserId, fromUserName);

        // Then
        ArgumentCaptor<String> channelCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).convertAndSend(channelCaptor.capture(), messageCaptor.capture());

        Assert.assertEquals(channelCaptor.getValue(), "user:connected:user123");
        
        String jsonPayload = messageCaptor.getValue();
        Assert.assertTrue(jsonPayload.contains("\"type\":\"NOTIFICATION\""));
        Assert.assertTrue(jsonPayload.contains("\"subType\":\"FRIEND_REQUEST\""));
        Assert.assertTrue(jsonPayload.contains("\"id\":\"sender456\""));
        Assert.assertTrue(jsonPayload.contains("\"name\":\"Alice\""));
    }

    @Test
    public void publishRequestAccepted_PublishesToCorrectChannel() {
        // Given
        String targetUserId = "requester123";
        String newFriendId = "accepter456";
        String newFriendName = "Bob";
        boolean isOnline = true;

        // When
        publisher.publishRequestAccepted(targetUserId, newFriendId, newFriendName, isOnline);

        // Then
        ArgumentCaptor<String> channelCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).convertAndSend(channelCaptor.capture(), messageCaptor.capture());

        Assert.assertEquals(channelCaptor.getValue(), "user:connected:requester123");
        
        String jsonPayload = messageCaptor.getValue();
        Assert.assertTrue(jsonPayload.contains("\"type\":\"NOTIFICATION\""));
        Assert.assertTrue(jsonPayload.contains("\"subType\":\"REQUEST_ACCEPTED\""));
        Assert.assertTrue(jsonPayload.contains("\"id\":\"accepter456\""));
        Assert.assertTrue(jsonPayload.contains("\"name\":\"Bob\""));
        // Note: Lombok's boolean field 'isOnline' generates getter 'isOnline()', 
        // which Jackson serializes as 'online' (without 'is' prefix)
        Assert.assertTrue(jsonPayload.contains("\"online\":true"));
    }

    @Test
    public void publishRequestAccepted_IncludesOfflineStatus() {
        // Given
        String targetUserId = "requester123";
        String newFriendId = "accepter456";
        String newFriendName = "Carol";
        boolean isOnline = false;

        // When
        publisher.publishRequestAccepted(targetUserId, newFriendId, newFriendName, isOnline);

        // Then
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).convertAndSend(anyString(), messageCaptor.capture());

        String jsonPayload = messageCaptor.getValue();
        Assert.assertTrue(jsonPayload.contains("\"online\":false"));
    }

    @Test
    public void publish_HandlesSpecialCharactersInUserName() {
        // Given
        String targetUserId = "user123";
        String fromUserId = "sender456";
        String fromUserName = "Alice \"The Great\" O'Brien";

        // When
        publisher.publishFriendRequest(targetUserId, fromUserId, fromUserName);

        // Then - should not throw, Jackson handles escaping
        verify(redisTemplate).convertAndSend(anyString(), anyString());
    }

    @Test
    public void getChannelForUser_ReturnsCorrectFormat() {
        // When
        String channel = RedisNotificationPublisher.getChannelForUser("user123");

        // Then
        Assert.assertEquals(channel, "user:connected:user123");
    }

    @Test
    public void publish_ThrowsOnSerializationError() throws JsonProcessingException {
        // Given
        ObjectMapper failingMapper = mock(ObjectMapper.class);
        when(failingMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("Serialization failed") {});
        
        RedisNotificationPublisher publisherWithFailingMapper = 
                new RedisNotificationPublisher(redisTemplate, failingMapper);

        NotificationPayload payload = NotificationPayload.friendRequest("id", "name");

        // When & Then
        try {
            publisherWithFailingMapper.publish("targetUser", payload);
            Assert.fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            Assert.assertTrue(e.getMessage().contains("serialize"));
        }

        // Verify Redis was not called
        verify(redisTemplate, never()).convertAndSend(anyString(), anyString());
    }
}
