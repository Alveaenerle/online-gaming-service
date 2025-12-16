package com.online_games_service.authorization.repository.redis;

import com.online_games_service.authorization.model.User;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Repository responsible for managing User sessions in Redis.
 * <p>
 * This repository implements the data access layer for the "Stateful Session" pattern.
 * Instead of storing session data in the application memory (which is not scalable),
 * sessions are serialized and stored in Redis.
 * </p>
 *
 * <h3>Storage Strategy:</h3>
 * <ul>
 * <li><strong>Key Pattern:</strong> {@code "auth:session:{sessionId}"} - This namespacing prevents collisions with other data in Redis.</li>
 * <li><strong>Value:</strong> The serialized {@link User} object.</li>
 * <li><strong>Expiration:</strong> Keys are automatically deleted by Redis after the configured timeout (TTL).</li>
 * </ul>
 */
@Repository
public class SessionRedisRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String KEY_PREFIX = "auth:session:";
    
    @Value("${onlinegamesservice.app.sessionTimeout:86400}")
    private long sessionTimeout;

    public SessionRedisRepository(@Qualifier("redisTemplate") RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Saves a user session in Redis with a specific Time-To-Live (TTL).
     * <p>
     * This effectively "logs in" the user. If a session with this ID already exists,
     * it will be overwritten and the expiration timer will be reset.
     * </p>
     *
     * @param sessionId The unique identifier for the session (UUID).
     * @param user      The user object to serialize and store.
     */
    public void saveSession(String sessionId, User user) {
        String key = KEY_PREFIX + sessionId;
        redisTemplate.opsForValue().set(key, user, sessionTimeout, TimeUnit.SECONDS);
    }

    /**
     * Retrieves a user by their session ID.
     * <p>
     * If the session has expired (TTL reached 0) or was deleted, Redis returns null,
     * and this method returns {@link Optional#empty()}.
     * </p>
     *
     * @param sessionId The unique session identifier to look up.
     * @return An {@link Optional} containing the User if found and valid, or empty otherwise.
     */
    public Optional<User> findUserBySessionId(String sessionId) {
        String key = KEY_PREFIX + sessionId;
        Object value = redisTemplate.opsForValue().get(key);
        if (value instanceof User) {
            return Optional.of((User) value);
        }
        return Optional.empty();
    }

    /**
     * Deletes a session from Redis.
     * <p>
     * This is used for "Logout" functionality. It immediately removes the key,
     * invalidating the session regardless of the remaining TTL.
     * </p>
     *
     * @param sessionId The unique session identifier to remove.
     */
    public void deleteSession(String sessionId) {
        String key = KEY_PREFIX + sessionId;
        redisTemplate.delete(key);
    }
}