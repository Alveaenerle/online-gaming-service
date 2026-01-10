package com.online_games_service.social.service;

import com.online_games_service.social.config.SocialRedisConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * Redis listener for key expiration events.
 * Listens to the __keyevent@0__:expired channel to detect when a user's
 * presence key expires (ghost disconnect / network loss scenario).
 * 
 * Note: Redis must be configured with notify-keyspace-events Ex to enable
 * keyspace notifications for expired events.
 */
@Component
public class RedisKeyExpirationListener implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(RedisKeyExpirationListener.class);

    private final PresenceService presenceService;
    private final FriendNotificationService friendNotificationService;

    public RedisKeyExpirationListener(
            PresenceService presenceService,
            @Lazy FriendNotificationService friendNotificationService) {
        this.presenceService = presenceService;
        this.friendNotificationService = friendNotificationService;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = new String(message.getBody());
        
        logger.debug("Redis key expired: {}", expiredKey);

        // Check if the expired key matches our presence pattern
        if (expiredKey.startsWith(SocialRedisConfig.ONLINE_USER_KEY_PREFIX)) {
            String userId = presenceService.extractUserIdFromKey(expiredKey);
            
            if (userId != null) {
                logger.info("User {} presence expired (ghost disconnect detected)", userId);
                handleUserOffline(userId);
            }
        }
    }

    private void handleUserOffline(String userId) {
        try {
            friendNotificationService.notifyFriendsUserOffline(userId);
        } catch (Exception e) {
            logger.error("Error notifying friends about user {} going offline: {}", userId, e.getMessage(), e);
        }
    }
}
