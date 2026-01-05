package com.online_games_service.makao.repository.redis;

import com.online_games_service.makao.model.MakaoGame;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ValueOperations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class MakaoGameRedisRepositoryTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private Cursor<String> cursor;

    private MakaoGameRedisRepository repository;
    private final String KEY_PREFIX = "makao:game:";

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        repository = new MakaoGameRedisRepository(redisTemplate, KEY_PREFIX);
    }

    @Test
    public void testGetKeyPrefix() {
        Assert.assertEquals(repository.getKeyPrefix(), KEY_PREFIX);
    }

    @Test
    public void testSave() {
        MakaoGame game = new MakaoGame();
        game.setRoomId("game1");

        MakaoGame savedGame = repository.save(game);

        Assert.assertEquals(savedGame, game);
        verify(valueOperations).set(eq(KEY_PREFIX + "game1"), eq(game), eq(3600L), eq(TimeUnit.SECONDS));
    }

    @Test
    public void testFindById_Found() {
        String gameId = "game1";
        MakaoGame game = new MakaoGame();
        game.setRoomId(gameId);

        when(valueOperations.get(KEY_PREFIX + gameId)).thenReturn(game);

        Optional<MakaoGame> result = repository.findById(gameId);

        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(result.get(), game);
    }

    @Test
    public void testFindById_NotFound() {
        String gameId = "game1";

        when(valueOperations.get(KEY_PREFIX + gameId)).thenReturn(null);

        Optional<MakaoGame> result = repository.findById(gameId);

        Assert.assertFalse(result.isPresent());
    }

    @Test
    public void testFindById_WrongType() {
        String gameId = "game1";

        when(valueOperations.get(KEY_PREFIX + gameId)).thenReturn("Not a game object");

        Optional<MakaoGame> result = repository.findById(gameId);

        Assert.assertFalse(result.isPresent());
    }

    @Test
    public void testExistsById_True() {
        String gameId = "game1";
        when(redisTemplate.hasKey(KEY_PREFIX + gameId)).thenReturn(true);

        boolean exists = repository.existsById(gameId);

        Assert.assertTrue(exists);
    }

    @Test
    public void testExistsById_False() {
        String gameId = "game1";
        when(redisTemplate.hasKey(KEY_PREFIX + gameId)).thenReturn(false);

        boolean exists = repository.existsById(gameId);

        Assert.assertFalse(exists);
    }

    @Test
    public void testExistsById_Null() {
        String gameId = "game1";
        when(redisTemplate.hasKey(KEY_PREFIX + gameId)).thenReturn(null);

        boolean exists = repository.existsById(gameId);

        Assert.assertFalse(exists);
    }

    @Test
    public void testDeleteById() {
        String gameId = "game1";

        repository.deleteById(gameId);

        verify(redisTemplate).delete(KEY_PREFIX + gameId);
    }

    @Test
    public void testCountGames() {
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);
        when(cursor.hasNext()).thenReturn(true, true, true, false);
        when(cursor.next()).thenReturn("key1", "key2", "key3");

        long count = repository.countGames();

        Assert.assertEquals(count, 3);
        verify(redisTemplate).scan(any(ScanOptions.class));
        verify(cursor, times(4)).hasNext();
        verify(cursor, times(3)).next();
        verify(cursor).close();
    }
}
