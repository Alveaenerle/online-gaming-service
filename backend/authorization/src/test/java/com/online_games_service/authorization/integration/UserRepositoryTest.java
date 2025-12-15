package com.online_games_service.authorization.integration;

import com.online_games_service.authorization.model.User;
import com.online_games_service.authorization.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Optional;

@SpringBootTest
@ActiveProfiles("test")
public class UserRepositoryTest extends AbstractTestNGSpringContextTests {

    @Autowired
    private UserRepository userRepository;

    @BeforeMethod
    public void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    public void shouldSaveAndRetrieveUser() {
        // Given
        User user = new User("SuperPlayer", false);

        // When
        User savedUser = userRepository.save(user);

        // Then
        Assert.assertNotNull(savedUser.getId());
        
        Optional<User> retrieved = userRepository.findById(savedUser.getId());
        Assert.assertTrue(retrieved.isPresent());
        Assert.assertEquals(retrieved.get().getUsername(), "SuperPlayer");
        Assert.assertFalse(retrieved.get().isGuest());
    }

    @Test
    public void shouldSaveGuestUser() {
        // Given
        User guest = new User("Guest_12345", true);

        // When
        User savedGuest = userRepository.save(guest);

        // Then
        Assert.assertNotNull(savedGuest.getId());
        Assert.assertTrue(savedGuest.isGuest());
    }

    @Test
    public void shouldFindUserByUsername() {
        // Given
        String username = "SearchMe";
        userRepository.save(new User(username, false));

        // When
        Optional<User> found = userRepository.findByUsername(username);

        // Then
        Assert.assertTrue(found.isPresent());
        Assert.assertEquals(found.get().getUsername(), username);
    }
    
    @Test
    public void shouldReturnEmptyWhenUserNotFound() {
        // When
        Optional<User> found = userRepository.findByUsername("GhostUser");

        // Then
        Assert.assertFalse(found.isPresent());
    }
}