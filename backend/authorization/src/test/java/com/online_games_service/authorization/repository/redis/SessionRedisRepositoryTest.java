package com.online_games_service.authorization.repository.redis;

import com.online_games_service.authorization.model.User;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class SessionRedisRepositoryTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private SessionRedisRepository repository;
    private final String KEY_PREFIX = "auth:session:";

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        repository = new SessionRedisRepository(redisTemplate);
    }

    @Test
    public void testSave() {
        String sessionId = "session1";
        User user = new User("1", "user", false);

        repository.saveSession(sessionId, user);

        verify(valueOperations).set(eq(KEY_PREFIX + sessionId), eq(user), anyLong(), eq(TimeUnit.SECONDS));
    }

    @Test
    public void testFindById_Found() {
        String sessionId = "session1";
        User user = new User("1", "user", false);

        when(valueOperations.get(KEY_PREFIX + sessionId)).thenReturn(user);

        Optional<User> result = repository.findUserBySessionId(sessionId);

        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(result.get(), user);
    }

    @Test
    public void testFindById_NotFound() {
        String sessionId = "session1";

        when(valueOperations.get(KEY_PREFIX + sessionId)).thenReturn(null);

        Optional<User> result = repository.findUserBySessionId(sessionId);

        Assert.assertFalse(result.isPresent());
    }

    @Test
    public void testFindById_WrongType() {
        String sessionId = "session1";

        when(valueOperations.get(KEY_PREFIX + sessionId)).thenReturn("Not a User object");

        Optional<User> result = repository.findUserBySessionId(sessionId);

        Assert.assertFalse(result.isPresent());
    }

    @Test
    public void testDeleteById() {
        String sessionId = "session1";

        repository.deleteSession(sessionId);

        verify(redisTemplate).delete(KEY_PREFIX + sessionId);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testSave_Exception() {
        String sessionId = "session1";
        User user = new User("1", "user", false);

        doThrow(new RuntimeException("Redis error")).when(valueOperations).set(anyString(), any(), anyLong(), any());

        repository.saveSession(sessionId, user);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testFindById_Exception() {
        String sessionId = "session1";

        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis error"));

        repository.findUserBySessionId(sessionId);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testDeleteById_Exception() {
        String sessionId = "session1";

        when(redisTemplate.delete(anyString())).thenThrow(new RuntimeException("Redis error"));

        repository.deleteSession(sessionId);
    }
}
