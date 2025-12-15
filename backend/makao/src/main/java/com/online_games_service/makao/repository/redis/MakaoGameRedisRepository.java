package com.online_games_service.makao.repository.redis;

import com.online_games_service.makao.model.MakaoGame;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Repository
public class MakaoGameRedisRepository {

    private static final String KEY_PREFIX = "MakaoGame:";
    private static final long TTL_SECONDS = 3600;

    private final RedisTemplate<String, Object> redisTemplate;

    public MakaoGameRedisRepository(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public MakaoGame save(MakaoGame game) {
        String key = KEY_PREFIX + game.getId();
        redisTemplate.opsForValue().set(key, game, TTL_SECONDS, TimeUnit.SECONDS);
        return game;
    }

    public Optional<MakaoGame> findById(String id) {
        String key = KEY_PREFIX + id;
        Object value = redisTemplate.opsForValue().get(key);
        if (value instanceof MakaoGame) {
            return Optional.of((MakaoGame) value);
        }
        return Optional.empty();
    }

    public boolean existsById(String id) {
        String key = KEY_PREFIX + id;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void deleteById(String id) {
        String key = KEY_PREFIX + id;
        redisTemplate.delete(key);
    }

    public void deleteAll() {
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    public long count() {
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        return keys != null ? keys.size() : 0;
    }
}
