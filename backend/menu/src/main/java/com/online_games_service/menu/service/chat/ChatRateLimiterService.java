package com.online_games_service.menu.service.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Service for rate limiting chat messages using Redis sliding window algorithm.
 * Limits users to X messages per Y seconds.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatRateLimiterService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Value("${chat.rate-limit.max-messages:5}")
    private int maxMessages;
    
    @Value("${chat.rate-limit.window-seconds:10}")
    private int windowSeconds;
    
    private static final String RATE_LIMIT_KEY_PREFIX = "chat:rate-limit:";
    
    /**
     * Checks if a user is rate limited and records the attempt.
     * Uses a sliding window counter with Redis sorted sets.
     * 
     * @param userId The user ID to check
     * @return Optional containing remaining wait time in ms if rate limited, empty if allowed
     */
    public Optional<Long> checkRateLimit(String userId) {
        String key = RATE_LIMIT_KEY_PREFIX + userId;
        long now = Instant.now().toEpochMilli();
        long windowStart = now - (windowSeconds * 1000L);
        
        // Remove expired entries
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);
        
        // Count current entries in window
        Long count = redisTemplate.opsForZSet().zCard(key);
        
        if (count != null && count >= maxMessages) {
            // User is rate limited - calculate when they can send again
            var oldestEntries = redisTemplate.opsForZSet().rangeWithScores(key, 0, 0);
            if (oldestEntries != null && !oldestEntries.isEmpty()) {
                var oldest = oldestEntries.iterator().next();
                if (oldest.getScore() != null) {
                    long oldestTime = oldest.getScore().longValue();
                    long retryAfter = (oldestTime + (windowSeconds * 1000L)) - now;
                    log.debug("User {} is rate limited, retry after {}ms", userId, retryAfter);
                    return Optional.of(Math.max(0, retryAfter));
                }
            }
            return Optional.of((long) windowSeconds * 1000);
        }
        
        // Add current request to the window
        redisTemplate.opsForZSet().add(key, now + ":" + userId, now);
        redisTemplate.expire(key, Duration.ofSeconds(windowSeconds + 1));
        
        return Optional.empty();
    }
    
    /**
     * Gets the current rate limit status for a user without recording an attempt.
     */
    public RateLimitStatus getStatus(String userId) {
        String key = RATE_LIMIT_KEY_PREFIX + userId;
        long now = Instant.now().toEpochMilli();
        long windowStart = now - (windowSeconds * 1000L);
        
        // Remove expired entries
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);
        
        Long count = redisTemplate.opsForZSet().zCard(key);
        int remaining = Math.max(0, maxMessages - (count != null ? count.intValue() : 0));
        
        return new RateLimitStatus(remaining, maxMessages, windowSeconds);
    }
    
    public record RateLimitStatus(int remaining, int limit, int windowSeconds) {}
}
