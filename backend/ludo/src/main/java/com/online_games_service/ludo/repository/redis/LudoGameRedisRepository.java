package com.online_games_service.ludo.repository.redis;

import com.online_games_service.ludo.model.LudoGame;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Repository
public class LudoGameRedisRepository {

    private final String keyPrefix;
    private static final long TTL_SECONDS = 3600; 

    private final RedisTemplate<String, Object> redisTemplate;

    public LudoGameRedisRepository(RedisTemplate<String, Object> redisTemplate,
                                   @Value("${ludo.redis.key-prefix}") String keyPrefix) {
        this.redisTemplate = redisTemplate;
        this.keyPrefix = keyPrefix;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public LudoGame save(LudoGame game) {
        String key = keyPrefix + game.getId();
        redisTemplate.opsForValue().set(key, game, TTL_SECONDS, TimeUnit.SECONDS);
        return game;
    }

    public Optional<LudoGame> findById(String id) {
        String key = keyPrefix + id;
        Object value = redisTemplate.opsForValue().get(key);
        if (value instanceof LudoGame) {
            return Optional.of((LudoGame) value);
        }
        return Optional.empty();
    }

    public boolean existsById(String id) {
        String key = keyPrefix + id;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void deleteById(String id) {
        String key = keyPrefix + id;
        redisTemplate.delete(key);
    }

    public long countGames() {
        long count = 0;
        ScanOptions options = ScanOptions.scanOptions().match(keyPrefix + "*").count(1000).build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                cursor.next();
                count++;
            }
        }
        return count;
    }
}