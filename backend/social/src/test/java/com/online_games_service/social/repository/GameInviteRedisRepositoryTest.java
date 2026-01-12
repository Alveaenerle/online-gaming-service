package com.online_games_service.social.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.online_games_service.social.model.GameInvite;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;

/**
 * Unit tests for GameInviteRedisRepository.
 */
public class GameInviteRedisRepositoryTest {

    private GameInviteRedisRepository repository;
    private RedisTemplate<String, Object> redisTemplate;
    private ObjectMapper objectMapper;
    private ValueOperations<String, Object> valueOperations;
    private SetOperations<String, Object> setOperations;

    @BeforeMethod
    @SuppressWarnings("unchecked")
    public void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        objectMapper = new ObjectMapper();
        valueOperations = mock(ValueOperations.class);
        setOperations = mock(SetOperations.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        repository = new GameInviteRedisRepository(redisTemplate, objectMapper);
    }

    // ============================================================
    // SAVE TESTS
    // ============================================================

    @Test
    public void save_ShouldStoreInviteWithCorrectKeys() throws JsonProcessingException {
        // Given
        GameInvite invite = GameInvite.create("sender1", "SenderName", "target1", 
                                              "lobby1", "Test Lobby", "MAKAO", "ABC123");

        // When
        GameInvite result = repository.save(invite);

        // Then
        Assert.assertEquals(result, invite);

        // Verify main key was set with TTL
        verify(valueOperations).set(
                eq("game:invite:" + invite.getId()),
                any(String.class),
                eq(1L),
                eq(TimeUnit.HOURS)
        );

        // Verify user set was updated
        verify(setOperations).add("game:invites:user:target1", invite.getId());

        // Verify lobby set was updated
        verify(setOperations).add("game:invites:lobby:lobby1", invite.getId());

        // Verify sender key was set
        verify(valueOperations).set(
                eq("game:invites:sender:sender1:target1:lobby1"),
                eq(invite.getId()),
                eq(1L),
                eq(TimeUnit.HOURS)
        );
    }

    // ============================================================
    // FIND BY ID TESTS
    // ============================================================

    @Test
    public void findById_WhenExists_ShouldReturnInvite() throws JsonProcessingException {
        // Given
        GameInvite invite = GameInvite.builder()
                .id("invite123")
                .senderId("sender")
                .targetId("target")
                .lobbyId("lobby")
                .build();
        String json = objectMapper.writeValueAsString(invite);
        when(valueOperations.get("game:invite:invite123")).thenReturn(json);

        // When
        Optional<GameInvite> result = repository.findById("invite123");

        // Then
        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(result.get().getId(), "invite123");
        Assert.assertEquals(result.get().getSenderId(), "sender");
    }

    @Test
    public void findById_WhenNotExists_ShouldReturnEmpty() {
        // Given
        when(valueOperations.get("game:invite:nonexistent")).thenReturn(null);

        // When
        Optional<GameInvite> result = repository.findById("nonexistent");

        // Then
        Assert.assertFalse(result.isPresent());
    }

    @Test
    public void findById_WhenInvalidJson_ShouldReturnEmpty() {
        // Given
        when(valueOperations.get("game:invite:badJson")).thenReturn("not valid json");

        // When
        Optional<GameInvite> result = repository.findById("badJson");

        // Then
        Assert.assertFalse(result.isPresent());
    }

    // ============================================================
    // FIND BY TARGET ID TESTS
    // ============================================================

    @Test
    public void findByTargetId_WhenHasInvites_ShouldReturnList() throws JsonProcessingException {
        // Given
        String targetId = "user123";
        GameInvite invite1 = GameInvite.builder()
                .id("inv1").senderId("s1").targetId(targetId).lobbyId("l1").build();
        GameInvite invite2 = GameInvite.builder()
                .id("inv2").senderId("s2").targetId(targetId).lobbyId("l2").build();

        Set<Object> inviteIds = new HashSet<>(Arrays.asList("inv1", "inv2"));
        when(setOperations.members("game:invites:user:" + targetId)).thenReturn(inviteIds);
        when(valueOperations.get("game:invite:inv1")).thenReturn(objectMapper.writeValueAsString(invite1));
        when(valueOperations.get("game:invite:inv2")).thenReturn(objectMapper.writeValueAsString(invite2));

        // When
        List<GameInvite> result = repository.findByTargetId(targetId);

        // Then
        Assert.assertEquals(result.size(), 2);
    }

    @Test
    public void findByTargetId_WhenNoInvites_ShouldReturnEmptyList() {
        // Given
        when(setOperations.members("game:invites:user:user123")).thenReturn(null);

        // When
        List<GameInvite> result = repository.findByTargetId("user123");

        // Then
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void findByTargetId_WhenEmptySet_ShouldReturnEmptyList() {
        // Given
        when(setOperations.members("game:invites:user:user123")).thenReturn(Collections.emptySet());

        // When
        List<GameInvite> result = repository.findByTargetId("user123");

        // Then
        Assert.assertTrue(result.isEmpty());
    }

    // ============================================================
    // FIND BY SENDER ID TESTS
    // ============================================================

    @Test
    public void findBySenderId_WhenHasInvites_ShouldReturnFiltered() throws JsonProcessingException {
        // Given
        GameInvite invite1 = GameInvite.builder()
                .id("inv1").senderId("sender1").targetId("t1").lobbyId("l1").build();
        GameInvite invite2 = GameInvite.builder()
                .id("inv2").senderId("sender2").targetId("t2").lobbyId("l2").build();

        Set<String> keys = new HashSet<>(Arrays.asList("game:invite:inv1", "game:invite:inv2"));
        when(redisTemplate.keys("game:invite:*")).thenReturn(keys);
        when(valueOperations.get("game:invite:inv1")).thenReturn(objectMapper.writeValueAsString(invite1));
        when(valueOperations.get("game:invite:inv2")).thenReturn(objectMapper.writeValueAsString(invite2));

        // When
        List<GameInvite> result = repository.findBySenderId("sender1");

        // Then
        Assert.assertEquals(result.size(), 1);
        Assert.assertEquals(result.get(0).getSenderId(), "sender1");
    }

    @Test
    public void findBySenderId_WhenNoKeys_ShouldReturnEmptyList() {
        // Given
        when(redisTemplate.keys("game:invite:*")).thenReturn(null);

        // When
        List<GameInvite> result = repository.findBySenderId("sender1");

        // Then
        Assert.assertTrue(result.isEmpty());
    }

    // ============================================================
    // EXISTS BY SENDER AND TARGET TESTS
    // ============================================================

    @Test
    public void existsBySenderIdAndTargetIdAndLobbyId_WhenExists_ShouldReturnTrue() {
        // Given
        when(redisTemplate.hasKey("game:invites:sender:s1:t1:l1")).thenReturn(true);

        // When
        boolean result = repository.existsBySenderIdAndTargetIdAndLobbyId("s1", "t1", "l1");

        // Then
        Assert.assertTrue(result);
    }

    @Test
    public void existsBySenderIdAndTargetIdAndLobbyId_WhenNotExists_ShouldReturnFalse() {
        // Given
        when(redisTemplate.hasKey("game:invites:sender:s1:t1:l1")).thenReturn(false);

        // When
        boolean result = repository.existsBySenderIdAndTargetIdAndLobbyId("s1", "t1", "l1");

        // Then
        Assert.assertFalse(result);
    }

    // ============================================================
    // DELETE BY ID TESTS
    // ============================================================

    @Test
    public void deleteById_WhenExists_ShouldDeleteAllKeys() throws JsonProcessingException {
        // Given
        GameInvite invite = GameInvite.builder()
                .id("inv1")
                .senderId("sender1")
                .targetId("target1")
                .lobbyId("lobby1")
                .build();
        when(valueOperations.get("game:invite:inv1")).thenReturn(objectMapper.writeValueAsString(invite));

        // When
        repository.deleteById("inv1");

        // Then
        verify(redisTemplate).delete("game:invite:inv1");
        verify(setOperations).remove("game:invites:user:target1", "inv1");
        verify(setOperations).remove("game:invites:lobby:lobby1", "inv1");
        verify(redisTemplate).delete("game:invites:sender:sender1:target1:lobby1");
    }

    @Test
    public void deleteById_WhenNotExists_ShouldDoNothing() {
        // Given
        when(valueOperations.get("game:invite:nonexistent")).thenReturn(null);

        // When
        repository.deleteById("nonexistent");

        // Then - only findById called, no deletions
        verify(redisTemplate, never()).delete(anyString());
    }

    // ============================================================
    // DELETE ALL BY LOBBY ID TESTS
    // ============================================================

    @Test
    public void deleteAllByLobbyId_WhenHasInvites_ShouldDeleteAll() throws JsonProcessingException {
        // Given
        String lobbyId = "lobby123";
        GameInvite invite1 = GameInvite.builder()
                .id("inv1").senderId("s1").targetId("t1").lobbyId(lobbyId).build();
        GameInvite invite2 = GameInvite.builder()
                .id("inv2").senderId("s2").targetId("t2").lobbyId(lobbyId).build();

        Set<Object> inviteIds = new HashSet<>(Arrays.asList("inv1", "inv2"));
        when(setOperations.members("game:invites:lobby:" + lobbyId)).thenReturn(inviteIds);
        when(valueOperations.get("game:invite:inv1")).thenReturn(objectMapper.writeValueAsString(invite1));
        when(valueOperations.get("game:invite:inv2")).thenReturn(objectMapper.writeValueAsString(invite2));

        // When
        int count = repository.deleteAllByLobbyId(lobbyId);

        // Then
        Assert.assertEquals(count, 2);
        verify(redisTemplate).delete("game:invites:lobby:" + lobbyId);
    }

    @Test
    public void deleteAllByLobbyId_WhenNoInvites_ShouldReturnZero() {
        // Given
        when(setOperations.members("game:invites:lobby:empty")).thenReturn(null);

        // When
        int count = repository.deleteAllByLobbyId("empty");

        // Then
        Assert.assertEquals(count, 0);
    }
}
