package com.online_games_service.social.service;

import com.online_games_service.social.config.SocialRedisConfig;
import com.online_games_service.social.dto.UserPresenceStatus;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service responsible for managing user presence status in Redis.
 * Handles setting online status with TTL, removing status, and bulk queries.
 */
@Service
public class PresenceService {

    private final StringRedisTemplate redisTemplate;

    public PresenceService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Sets a user as online in Redis with TTL expiration.
     * Called when user connects or sends a heartbeat PING.
     *
     * @param userId The user ID to set as online
     */
    public void setUserOnline(String userId) {
        String key = buildOnlineKey(userId);
        redisTemplate.opsForValue().set(
                key,
                "true",
                Duration.ofSeconds(SocialRedisConfig.PRESENCE_TTL_SECONDS)
        );
    }

    /**
     * Removes a user's online status from Redis immediately.
     * Called when user explicitly logs out or disconnects gracefully.
     *
     * @param userId The user ID to remove from online status
     */
    public void removeUserOnline(String userId) {
        String key = buildOnlineKey(userId);
        redisTemplate.delete(key);
    }

    /**
     * Checks if a single user is currently online.
     *
     * @param userId The user ID to check
     * @return true if the user is online, false otherwise
     */
    public boolean isUserOnline(String userId) {
        String key = buildOnlineKey(userId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Gets the online status of multiple users in a single Redis MGET operation.
     * This is optimized for the Friends Drawer to fetch 50+ friends efficiently.
     *
     * @param userIds List of user IDs to check
     * @return List of UserPresenceStatus with online flag for each user
     */
    public List<UserPresenceStatus> getUsersOnlineStatus(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> keys = userIds.stream()
                .map(this::buildOnlineKey)
                .collect(Collectors.toList());

        List<String> values = redisTemplate.opsForValue().multiGet(keys);

        List<UserPresenceStatus> result = new ArrayList<>();
        for (int i = 0; i < userIds.size(); i++) {
            String userId = userIds.get(i);
            boolean isOnline = values != null && i < values.size() && values.get(i) != null;
            result.add(new UserPresenceStatus(userId, isOnline));
        }

        return result;
    }

    /**
     * Refreshes the TTL for a user's online status.
     * Called when receiving a heartbeat PING to extend the session.
     *
     * @param userId The user ID whose TTL to refresh
     * @return true if the key existed and TTL was refreshed, false otherwise
     */
    public boolean refreshUserPresence(String userId) {
        String key = buildOnlineKey(userId);
        Boolean result = redisTemplate.expire(key, Duration.ofSeconds(SocialRedisConfig.PRESENCE_TTL_SECONDS));
        if (Boolean.FALSE.equals(result)) {
            // Key didn't exist, set it
            setUserOnline(userId);
            return true;
        }
        return Boolean.TRUE.equals(result);
    }

    /**
     * Extracts the user ID from a Redis key.
     * Used by the expiration listener to identify which user went offline.
     *
     * @param key The Redis key in format "online:user:{userId}"
     * @return The extracted user ID, or null if key doesn't match pattern
     */
    public String extractUserIdFromKey(String key) {
        if (key != null && key.startsWith(SocialRedisConfig.ONLINE_USER_KEY_PREFIX)) {
            return key.substring(SocialRedisConfig.ONLINE_USER_KEY_PREFIX.length());
        }
        return null;
    }

    private String buildOnlineKey(String userId) {
        return SocialRedisConfig.ONLINE_USER_KEY_PREFIX + userId;
    }
}
