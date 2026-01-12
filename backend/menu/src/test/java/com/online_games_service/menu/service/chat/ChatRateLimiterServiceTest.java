package com.online_games_service.menu.service.chat;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class ChatRateLimiterServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    private ChatRateLimiterService rateLimiter;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        rateLimiter = new ChatRateLimiterService(redisTemplate);
        // Set the @Value fields via reflection
        ReflectionTestUtils.setField(rateLimiter, "maxMessages", 5);
        ReflectionTestUtils.setField(rateLimiter, "windowSeconds", 10);
    }

    @Test
    public void shouldAllowMessageWhenUnderLimit() {
        String userId = "user-123";
        
        when(zSetOperations.zCard(anyString())).thenReturn(3L);  // 3 messages, limit is 5
        when(zSetOperations.add(anyString(), any(), anyDouble())).thenReturn(true);

        Optional<Long> result = rateLimiter.checkRateLimit(userId);

        Assert.assertTrue(result.isEmpty());
        verify(zSetOperations).add(anyString(), any(), anyDouble());
    }

    @Test
    public void shouldBlockMessageWhenAtLimit() {
        String userId = "user-456";
        
        when(zSetOperations.zCard(anyString())).thenReturn(5L);  // At limit
        
        // Mock oldest entry for retry calculation
        ZSetOperations.TypedTuple<Object> tuple = mock(ZSetOperations.TypedTuple.class);
        when(tuple.getScore()).thenReturn((double) System.currentTimeMillis() - 5000);
        Set<ZSetOperations.TypedTuple<Object>> tuples = new HashSet<>();
        tuples.add(tuple);
        when(zSetOperations.rangeWithScores(anyString(), eq(0L), eq(0L))).thenReturn(tuples);

        Optional<Long> result = rateLimiter.checkRateLimit(userId);

        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get() > 0);
        // Should not add new entry when rate limited
        verify(zSetOperations, never()).add(anyString(), any(), anyDouble());
    }

    @Test
    public void shouldRemoveExpiredEntries() {
        String userId = "user-789";
        
        when(zSetOperations.zCard(anyString())).thenReturn(0L);
        when(zSetOperations.add(anyString(), any(), anyDouble())).thenReturn(true);

        rateLimiter.checkRateLimit(userId);

        // Verify expired entries are removed
        verify(zSetOperations).removeRangeByScore(anyString(), eq(0.0), anyDouble());
    }

    @Test
    public void simulateRapidMessages_shouldBlockSixthMessage() {
        String userId = "rapid-user";
        
        // Simulate 5 messages already sent
        when(zSetOperations.zCard(anyString()))
                .thenReturn(0L)   // First call
                .thenReturn(1L)   // Second
                .thenReturn(2L)   // Third
                .thenReturn(3L)   // Fourth
                .thenReturn(4L)   // Fifth
                .thenReturn(5L);  // Sixth - should be blocked
        
        when(zSetOperations.add(anyString(), any(), anyDouble())).thenReturn(true);
        
        ZSetOperations.TypedTuple<Object> tuple = mock(ZSetOperations.TypedTuple.class);
        when(tuple.getScore()).thenReturn((double) System.currentTimeMillis());
        Set<ZSetOperations.TypedTuple<Object>> tuples = new HashSet<>();
        tuples.add(tuple);
        when(zSetOperations.rangeWithScores(anyString(), eq(0L), eq(0L))).thenReturn(tuples);

        // Send 5 messages - all should succeed
        for (int i = 0; i < 5; i++) {
            Optional<Long> result = rateLimiter.checkRateLimit(userId);
            Assert.assertTrue(result.isEmpty(), "Message " + (i + 1) + " should be allowed");
        }

        // 6th message should be blocked
        Optional<Long> blocked = rateLimiter.checkRateLimit(userId);
        Assert.assertTrue(blocked.isPresent(), "6th message should be rate limited");
    }

    @Test
    public void shouldGetRateLimitStatus() {
        String userId = "status-user";
        
        when(zSetOperations.zCard(anyString())).thenReturn(3L);

        var status = rateLimiter.getStatus(userId);

        Assert.assertEquals(status.remaining(), 2);  // 5 - 3 = 2 remaining
        Assert.assertEquals(status.limit(), 5);
    }
}
