package com.online_games_service.authorization.repository.redis;

import com.online_games_service.authorization.model.User;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Repository
public class SessionRedisRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    
    private final String keyPrefix = "auth:session:";
    
    @Value("${onlinegamesservice.app.sessionTimeout:86400}")
    private long sessionTimeout;

    public SessionRedisRepository(@Qualifier("redisTemplate") RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void saveSession(String sessionId, User user) {
        String key = keyPrefix + sessionId;
        redisTemplate.opsForValue().set(key, user, sessionTimeout, TimeUnit.SECONDS);
    }

    public Optional<User> findUserBySessionId(String sessionId) {
        String key = keyPrefix + sessionId;
        Object value = redisTemplate.opsForValue().get(key);
        if (value instanceof User) {
            return Optional.of((User) value);
        }
        return Optional.empty();
    }

    public void deleteSession(String sessionId) {
        String key = keyPrefix + sessionId;
        redisTemplate.delete(key);
    }
}