package com.online_games_service.social.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.online_games_service.social.dto.NotificationPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Service responsible for publishing notifications to Redis Pub/Sub channels.
 * Notifications are published to channel: user:connected:{userId}
 */
@Service
public class RedisNotificationPublisher {

    private static final Logger logger = LoggerFactory.getLogger(RedisNotificationPublisher.class);
    private static final String CHANNEL_PREFIX = "user:connected:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisNotificationPublisher(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publishes a friend request notification to the target user's channel.
     *
     * @param targetUserId The user ID to send the notification to
     * @param fromUserId   The ID of the user who sent the request
     * @param fromUserName The name of the user who sent the request
     */
    public void publishFriendRequest(String targetUserId, String fromUserId, String fromUserName) {
        NotificationPayload payload = NotificationPayload.friendRequest(fromUserId, fromUserName);
        publish(targetUserId, payload);
    }

    /**
     * Publishes a request accepted notification to the original requester's channel.
     *
     * @param targetUserId The user ID to send the notification to (the original requester)
     * @param newFriendId  The ID of the new friend (who accepted the request)
     * @param newFriendName The name of the new friend
     * @param isOnline     Whether the new friend is currently online
     */
    public void publishRequestAccepted(String targetUserId, String newFriendId, String newFriendName, boolean isOnline) {
        NotificationPayload payload = NotificationPayload.requestAccepted(newFriendId, newFriendName, isOnline);
        publish(targetUserId, payload);
    }

    /**
     * Publishes a notification payload to the user's Redis channel.
     *
     * @param userId  The target user ID
     * @param payload The notification payload
     */
    public void publish(String userId, NotificationPayload payload) {
        String channel = CHANNEL_PREFIX + userId;
        try {
            String json = objectMapper.writeValueAsString(payload);
            redisTemplate.convertAndSend(channel, json);
            logger.info("Published notification to channel {}: type={}, subType={}", 
                    channel, payload.getType(), payload.getSubType());
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize notification payload for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to serialize notification payload", e);
        }
    }

    /**
     * Gets the Redis channel name for a user.
     *
     * @param userId The user ID
     * @return The channel name
     */
    public static String getChannelForUser(String userId) {
        return CHANNEL_PREFIX + userId;
    }
}
