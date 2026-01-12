package com.online_games_service.social.integration;

import com.online_games_service.social.config.SocialRedisConfig;
import com.online_games_service.social.dto.UserPresenceStatus;
import com.online_games_service.social.model.SocialProfile;
import com.online_games_service.social.repository.SocialProfileRepository;
import com.online_games_service.social.service.PresenceService;
import com.online_games_service.test.BaseIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Integration tests for the presence system.
 * Tests the full flow of presence management with real Redis and MongoDB.
 */
@SpringBootTest
public class PresenceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private PresenceService presenceService;

    @Autowired
    private SocialProfileRepository socialProfileRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeMethod
    public void setUp() {
        // Clean up Redis keys
        var keys = redisTemplate.keys(SocialRedisConfig.ONLINE_USER_KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        
        // Clean up MongoDB
        socialProfileRepository.deleteAll();
    }

    @Test
    public void shouldTrackUserPresenceWithTTL() {
        // Given
        String userId = "integration_user";

        // When
        presenceService.setUserOnline(userId);

        // Then
        Assert.assertTrue(presenceService.isUserOnline(userId));
        
        String key = SocialRedisConfig.ONLINE_USER_KEY_PREFIX + userId;
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        Assert.assertNotNull(ttl);
        Assert.assertTrue(ttl > 0, "TTL should be positive");
        Assert.assertTrue(ttl <= SocialRedisConfig.PRESENCE_TTL_SECONDS, 
                "TTL should not exceed configured value");
    }

    @Test
    public void shouldBulkQueryFriendsStatus() {
        // Given - Create a user with friends in MongoDB
        String userId = "user_with_friends";
        String friend1 = "online_friend";
        String friend2 = "offline_friend";
        String friend3 = "another_online_friend";

        SocialProfile profile = new SocialProfile(userId);
        profile.setFriendIds(Set.of(friend1, friend2, friend3));
        socialProfileRepository.save(profile);

        // Set some friends as online
        presenceService.setUserOnline(friend1);
        presenceService.setUserOnline(friend3);

        // When - Query friends status
        List<String> friendIds = Arrays.asList(friend1, friend2, friend3);
        List<UserPresenceStatus> statuses = presenceService.getUsersOnlineStatus(friendIds);

        // Then
        Assert.assertEquals(statuses.size(), 3);
        
        UserPresenceStatus status1 = statuses.stream()
                .filter(s -> s.getUserId().equals(friend1)).findFirst().orElseThrow();
        Assert.assertTrue(status1.isOnline());
        
        UserPresenceStatus status2 = statuses.stream()
                .filter(s -> s.getUserId().equals(friend2)).findFirst().orElseThrow();
        Assert.assertFalse(status2.isOnline());
        
        UserPresenceStatus status3 = statuses.stream()
                .filter(s -> s.getUserId().equals(friend3)).findFirst().orElseThrow();
        Assert.assertTrue(status3.isOnline());
    }

    @Test
    public void shouldHandleExplicitLogout() {
        // Given
        String userId = "logout_user";
        presenceService.setUserOnline(userId);
        Assert.assertTrue(presenceService.isUserOnline(userId));

        // When
        presenceService.removeUserOnline(userId);

        // Then
        Assert.assertFalse(presenceService.isUserOnline(userId));
    }

    @Test
    public void shouldRefreshPresenceTTL() throws InterruptedException {
        // Given
        String userId = "refresh_ttl_user";
        presenceService.setUserOnline(userId);
        
        String key = SocialRedisConfig.ONLINE_USER_KEY_PREFIX + userId;
        
        // Wait a bit
        Thread.sleep(2000);
        Long ttlBefore = redisTemplate.getExpire(key, TimeUnit.SECONDS);

        // When
        presenceService.refreshUserPresence(userId);

        // Then
        Long ttlAfter = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        Assert.assertNotNull(ttlAfter);
        Assert.assertTrue(ttlAfter >= ttlBefore, "TTL should be refreshed");
    }

    @Test
    public void shouldHandleManyUsersEfficiently() {
        // Given - Set up 100 users
        int totalUsers = 100;
        List<String> allUserIds = new java.util.ArrayList<>();
        
        for (int i = 0; i < totalUsers; i++) {
            String userId = "bulk_user_" + i;
            allUserIds.add(userId);
            
            // Every other user is online
            if (i % 2 == 0) {
                presenceService.setUserOnline(userId);
            }
        }

        // When - Query all at once
        long startTime = System.currentTimeMillis();
        List<UserPresenceStatus> statuses = presenceService.getUsersOnlineStatus(allUserIds);
        long duration = System.currentTimeMillis() - startTime;

        // Then
        Assert.assertEquals(statuses.size(), totalUsers);
        
        int onlineCount = (int) statuses.stream().filter(UserPresenceStatus::isOnline).count();
        Assert.assertEquals(onlineCount, 50);
        
        // Should be fast (single MGET operation)
        Assert.assertTrue(duration < 1000, "Bulk query should be fast: " + duration + "ms");
    }

    @Test
    public void shouldExpirePresenceAfterTTL() throws InterruptedException {
        // Given - Set a very short TTL key directly
        String userId = "expiring_user";
        String key = SocialRedisConfig.ONLINE_USER_KEY_PREFIX + userId;
        
        // Set with 1 second TTL for testing
        redisTemplate.opsForValue().set(key, "true", Duration.ofSeconds(1));
        
        Assert.assertTrue(presenceService.isUserOnline(userId));

        // When - Wait for expiration
        Thread.sleep(1500);

        // Then
        Assert.assertFalse(presenceService.isUserOnline(userId));
    }

    @Test
    public void shouldCorrectlyExtractUserIdFromKey() {
        // Given
        String userId = "test_user_123";
        String key = SocialRedisConfig.ONLINE_USER_KEY_PREFIX + userId;

        // When
        String extractedUserId = presenceService.extractUserIdFromKey(key);

        // Then
        Assert.assertEquals(extractedUserId, userId);
    }

    @Test
    public void shouldWorkWithFriendsFromMongoDB() {
        // Given - Create two users who are friends
        String user1 = "user_alice";
        String user2 = "user_bob";

        SocialProfile profile1 = new SocialProfile(user1);
        profile1.addFriend(user2);
        socialProfileRepository.save(profile1);

        SocialProfile profile2 = new SocialProfile(user2);
        profile2.addFriend(user1);
        socialProfileRepository.save(profile2);

        // Set user1 online
        presenceService.setUserOnline(user1);

        // When - Get friends of user2 and check their status
        SocialProfile bobProfile = socialProfileRepository.findById(user2).orElseThrow();
        List<String> bobsFriends = List.copyOf(bobProfile.getFriendIds());
        
        List<UserPresenceStatus> statuses = presenceService.getUsersOnlineStatus(bobsFriends);

        // Then
        Assert.assertEquals(statuses.size(), 1);
        Assert.assertEquals(statuses.get(0).getUserId(), user1);
        Assert.assertTrue(statuses.get(0).isOnline());
    }
}
