package com.online_games_service.ludo.repository.redis;

import com.online_games_service.ludo.model.LudoGame;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class LudoGameRedisRepositoryTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private Cursor<String> cursor;

    private LudoGameRedisRepository repository;
    private final String KEY_PREFIX = "ludo:game:";

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        repository = new LudoGameRedisRepository(redisTemplate, KEY_PREFIX);
    }

    @Test
    public void testGetKeyPrefix() {
        // Given & When & Then
        Assert.assertEquals(repository.getKeyPrefix(), KEY_PREFIX);
    }

    @Test
    public void testSave() {
        // Given
        LudoGame game = new LudoGame();
        game.setRoomId("game1");

        // When
        LudoGame savedGame = repository.save(game);

        // Then
        Assert.assertEquals(savedGame, game);
        verify(valueOperations).set(eq(KEY_PREFIX + "game1"), eq(game), anyLong(), eq(TimeUnit.SECONDS));
    }

    @Test
    public void testCreateGameIfAbsent_Success() {
        // Given
        LudoGame game = new LudoGame();
        game.setRoomId("room1");
        
        when(valueOperations.setIfAbsent(eq(KEY_PREFIX + "room1"), eq(game), anyLong(), eq(TimeUnit.SECONDS)))
                .thenReturn(true);

        // When
        boolean result = repository.createGameIfAbsent(game);

        // Then
        Assert.assertTrue(result);
        verify(valueOperations).setIfAbsent(anyString(), any(), anyLong(), any());
    }

    @Test
    public void testCreateGameIfAbsent_AlreadyExists() {
        // Given
        LudoGame game = new LudoGame();
        game.setRoomId("room1");

        when(valueOperations.setIfAbsent(eq(KEY_PREFIX + "room1"), eq(game), anyLong(), eq(TimeUnit.SECONDS)))
                .thenReturn(false);

        // When
        boolean result = repository.createGameIfAbsent(game);

        // Then
        Assert.assertFalse(result);
    }

    @Test
    public void testCreateGameIfAbsent_NullReturn() {
        // Given
        LudoGame game = new LudoGame();
        game.setRoomId("room1");

        when(valueOperations.setIfAbsent(anyString(), any(), anyLong(), any()))
                .thenReturn(null);

        // When
        boolean result = repository.createGameIfAbsent(game);

        // Then
        Assert.assertFalse(result);
    }

    @Test
    public void testFindById_Found() {
        // Given
        String gameId = "game1";
        LudoGame game = new LudoGame();
        game.setRoomId(gameId);

        when(valueOperations.get(KEY_PREFIX + gameId)).thenReturn(game);

        // When
        Optional<LudoGame> result = repository.findById(gameId);

        // Then
        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(result.get(), game);
    }

    @Test
    public void testFindById_NotFound() {
        // Given
        String gameId = "game1";
        when(valueOperations.get(KEY_PREFIX + gameId)).thenReturn(null);

        // When
        Optional<LudoGame> result = repository.findById(gameId);

        // Then
        Assert.assertFalse(result.isPresent());
    }

    @Test
    public void testFindById_WrongType() {
        // Given
        String gameId = "game1";
        when(valueOperations.get(KEY_PREFIX + gameId)).thenReturn("Some String Object");

        // When
        Optional<LudoGame> result = repository.findById(gameId);

        // Then
        Assert.assertFalse(result.isPresent());
    }

    @Test
    public void testExistsById_True() {
        // Given
        String gameId = "game1";
        when(redisTemplate.hasKey(KEY_PREFIX + gameId)).thenReturn(true);

        // When
        boolean exists = repository.existsById(gameId);

        // Then
        Assert.assertTrue(exists);
    }

    @Test
    public void testExistsById_False() {
        // Given
        String gameId = "game1";
        when(redisTemplate.hasKey(KEY_PREFIX + gameId)).thenReturn(false);

        // When
        boolean exists = repository.existsById(gameId);

        // Then
        Assert.assertFalse(exists);
    }

    @Test
    public void testExistsById_Null() {
        // Given
        String gameId = "game1";
        when(redisTemplate.hasKey(KEY_PREFIX + gameId)).thenReturn(null);

        // When
        boolean exists = repository.existsById(gameId);

        // Then
        Assert.assertFalse(exists);
    }

    @Test
    public void testDeleteById() {
        // Given
        String gameId = "game1";
        
        // When
        repository.deleteById(gameId);
        
        // Then
        verify(redisTemplate).delete(KEY_PREFIX + gameId);
    }

    @Test
    public void testCountGames() {
        // Given
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);
        when(cursor.hasNext()).thenReturn(true, true, false); 
        when(cursor.next()).thenReturn("key1", "key2");

        // When
        long count = repository.countGames();

        // Then
        Assert.assertEquals(count, 2);
        verify(redisTemplate).scan(any(ScanOptions.class));
        verify(cursor, times(2)).next();
        verify(cursor).close();
    }
}