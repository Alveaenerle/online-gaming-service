package com.online_games_service.social.integration;

import com.online_games_service.social.model.FriendRequest;
import com.online_games_service.social.repository.FriendRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Optional;

@SpringBootTest
@ActiveProfiles("test")
public class FriendRequestRepositoryTest extends AbstractTestNGSpringContextTests {

    @Autowired
    private FriendRequestRepository friendRequestRepository;

    @BeforeMethod
    public void cleanUp() {
        friendRequestRepository.deleteAll();
    }

    @Test
    public void shouldSaveAndRetrieveRequest() {
        // Given
        FriendRequest request = new FriendRequest("userA", "userB");

        // When
        FriendRequest saved = friendRequestRepository.save(request);

        // Then
        Assert.assertNotNull(saved.getId());
        Optional<FriendRequest> found = friendRequestRepository.findByRequesterIdAndAddresseeId("userA", "userB");
        Assert.assertTrue(found.isPresent());
        Assert.assertEquals(found.get().getId(), saved.getId());
    }

    @Test
    public void shouldBlockDuplicateRequests() {
        // Given
        friendRequestRepository.save(new FriendRequest("userA", "userB"));

        // When & Then
        Assert.assertThrows(DuplicateKeyException.class, () -> {
            friendRequestRepository.save(new FriendRequest("userA", "userB"));
        });
    }

    @Test
    public void shouldFindAllIncomingRequests() {
        // Given
        friendRequestRepository.save(new FriendRequest("user1", "me"));
        friendRequestRepository.save(new FriendRequest("user2", "me"));
        friendRequestRepository.save(new FriendRequest("me", "user3"));

        // When
        List<FriendRequest> incoming = friendRequestRepository.findAllByAddresseeId("me");

        // Then
        Assert.assertEquals(incoming.size(), 2);

        Assert.assertTrue(incoming.stream().anyMatch(r -> r.getRequesterId().equals("user1")));
        Assert.assertTrue(incoming.stream().anyMatch(r -> r.getRequesterId().equals("user2")));
    }
}