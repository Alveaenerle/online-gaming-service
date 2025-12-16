package com.online_games_service.authorization.integration;

import com.online_games_service.authorization.model.User;
import com.online_games_service.authorization.repository.redis.SessionRedisRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Optional;
import java.util.UUID;

public class SessionRedisRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private SessionRedisRepository sessionRedisRepository;

    private String sessionId;

    @BeforeMethod
    public void setUp() {
        sessionId = UUID.randomUUID().toString();
    }

    @Test
    public void shouldSaveAndRetrieveUserSession() {
        // Given
        User user = new User("user-id-123", "SuperPlayer", false);

        // When
        sessionRedisRepository.saveSession(sessionId, user);

        // Then
        Optional<User> retrieved = sessionRedisRepository.findUserBySessionId(sessionId);
        Assert.assertTrue(retrieved.isPresent());
        Assert.assertEquals(retrieved.get().getUsername(), "SuperPlayer");
        Assert.assertEquals(retrieved.get().getId(), "user-id-123");
        Assert.assertFalse(retrieved.get().isGuest());
    }

    @Test
    public void shouldSaveGuestUserSession() {
        // Given
        User guest = new User("guest-id-999", "Guest_12345", true);

        // When
        sessionRedisRepository.saveSession(sessionId, guest);

        // Then
        Optional<User> retrieved = sessionRedisRepository.findUserBySessionId(sessionId);
        Assert.assertTrue(retrieved.isPresent());
        Assert.assertTrue(retrieved.get().isGuest());
    }

    @Test
    public void shouldReturnEmptyWhenSessionNotFound() {
        // When
        Optional<User> found = sessionRedisRepository.findUserBySessionId("non-existent-session-id");

        // Then
        Assert.assertFalse(found.isPresent());
    }

    @Test
    public void shouldDeleteSession() {
        // Given
        User user = new User("123", "UserToDelete", false);
        sessionRedisRepository.saveSession(sessionId, user);
        Assert.assertTrue(sessionRedisRepository.findUserBySessionId(sessionId).isPresent());

        // When
        sessionRedisRepository.deleteSession(sessionId);

        // Then
        Optional<User> deleted = sessionRedisRepository.findUserBySessionId(sessionId);
        Assert.assertFalse(deleted.isPresent());
    }
}