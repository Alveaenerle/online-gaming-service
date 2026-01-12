package com.online_games_service.social.service;

import com.online_games_service.social.config.SocialRedisConfig;
import com.online_games_service.social.dto.UserPresenceStatus;
import com.online_games_service.test.BaseIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@SpringBootTest
public class PresenceServiceTest extends BaseIntegrationTest {

    @Autowired
    private PresenceService presenceService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeMethod
    public void setUp() {
        // Clean up all online:user:* keys before each test
        var keys = redisTemplate.keys(SocialRedisConfig.ONLINE_USER_KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Test
    public void shouldSetUserOnline() {
        // Given
        String userId = "user123";

        // When
        presenceService.setUserOnline(userId);

        // Then
        Assert.assertTrue(presenceService.isUserOnline(userId));
        
        String key = SocialRedisConfig.ONLINE_USER_KEY_PREFIX + userId;
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        Assert.assertNotNull(ttl);
        Assert.assertTrue(ttl > 0 && ttl <= SocialRedisConfig.PRESENCE_TTL_SECONDS);
    }

    @Test
    public void shouldRemoveUserOnline() {
        // Given
        String userId = "user456";
        presenceService.setUserOnline(userId);
        Assert.assertTrue(presenceService.isUserOnline(userId));

        // When
        presenceService.removeUserOnline(userId);

        // Then
        Assert.assertFalse(presenceService.isUserOnline(userId));
    }

    @Test
    public void shouldReturnFalseForOfflineUser() {
        // Given
        String userId = "nonexistent_user";

        // When & Then
        Assert.assertFalse(presenceService.isUserOnline(userId));
    }

    @Test
    public void shouldGetMultipleUsersOnlineStatus() {
        // Given
        String onlineUser1 = "online_user_1";
        String onlineUser2 = "online_user_2";
        String offlineUser = "offline_user";
        
        presenceService.setUserOnline(onlineUser1);
        presenceService.setUserOnline(onlineUser2);

        List<String> userIds = Arrays.asList(onlineUser1, offlineUser, onlineUser2);

        // When
        List<UserPresenceStatus> statuses = presenceService.getUsersOnlineStatus(userIds);

        // Then
        Assert.assertEquals(statuses.size(), 3);
        
        Assert.assertEquals(statuses.get(0).getUserId(), onlineUser1);
        Assert.assertTrue(statuses.get(0).isOnline());
        
        Assert.assertEquals(statuses.get(1).getUserId(), offlineUser);
        Assert.assertFalse(statuses.get(1).isOnline());
        
        Assert.assertEquals(statuses.get(2).getUserId(), onlineUser2);
        Assert.assertTrue(statuses.get(2).isOnline());
    }

    @Test
    public void shouldHandleEmptyUserList() {
        // When
        List<UserPresenceStatus> statuses = presenceService.getUsersOnlineStatus(List.of());

        // Then
        Assert.assertNotNull(statuses);
        Assert.assertTrue(statuses.isEmpty());
    }

    @Test
    public void shouldHandleNullUserList() {
        // When
        List<UserPresenceStatus> statuses = presenceService.getUsersOnlineStatus(null);

        // Then
        Assert.assertNotNull(statuses);
        Assert.assertTrue(statuses.isEmpty());
    }

    @Test
    public void shouldRefreshUserPresence() {
        // Given
        String userId = "refresh_user";
        presenceService.setUserOnline(userId);
        
        // Wait a bit so TTL decreases
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        String key = SocialRedisConfig.ONLINE_USER_KEY_PREFIX + userId;
        Long ttlBefore = redisTemplate.getExpire(key, TimeUnit.SECONDS);

        // When
        presenceService.refreshUserPresence(userId);

        // Then
        Long ttlAfter = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        Assert.assertNotNull(ttlAfter);
        // TTL should be refreshed (close to max)
        Assert.assertTrue(ttlAfter >= ttlBefore);
    }

    @Test
    public void shouldRefreshAndCreateIfNotExists() {
        // Given
        String userId = "new_refresh_user";
        Assert.assertFalse(presenceService.isUserOnline(userId));

        // When
        boolean result = presenceService.refreshUserPresence(userId);

        // Then
        Assert.assertTrue(result);
        Assert.assertTrue(presenceService.isUserOnline(userId));
    }

    @Test
    public void shouldExtractUserIdFromValidKey() {
        // Given
        String key = "online:user:user123";

        // When
        String userId = presenceService.extractUserIdFromKey(key);

        // Then
        Assert.assertEquals(userId, "user123");
    }

    @Test
    public void shouldReturnNullForInvalidKeyPattern() {
        // Given
        String key = "some:other:key";

        // When
        String userId = presenceService.extractUserIdFromKey(key);

        // Then
        Assert.assertNull(userId);
    }

    @Test
    public void shouldHandleNullKeyExtraction() {
        // When
        String userId = presenceService.extractUserIdFromKey(null);

        // Then
        Assert.assertNull(userId);
    }

    @Test
    public void shouldHandleLargeNumberOfUsers() {
        // Given - set 100 users online
        List<String> userIds = new java.util.ArrayList<>();
        for (int i = 0; i < 100; i++) {
            String userId = "bulk_user_" + i;
            userIds.add(userId);
            if (i % 2 == 0) {
                presenceService.setUserOnline(userId);
            }
        }

        // When
        List<UserPresenceStatus> statuses = presenceService.getUsersOnlineStatus(userIds);

        // Then
        Assert.assertEquals(statuses.size(), 100);
        
        int onlineCount = 0;
        int offlineCount = 0;
        for (UserPresenceStatus status : statuses) {
            if (status.isOnline()) {
                onlineCount++;
            } else {
                offlineCount++;
            }
        }
        
        Assert.assertEquals(onlineCount, 50);
        Assert.assertEquals(offlineCount, 50);
    }
}
