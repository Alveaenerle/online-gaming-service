package com.online_games_service.social.integration;

import com.online_games_service.social.model.SocialProfile;
import com.online_games_service.social.repository.SocialProfileRepository;
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
public class SocialProfileRepositoryTest extends AbstractTestNGSpringContextTests {

    @Autowired
    private SocialProfileRepository socialProfileRepository;

    @BeforeMethod
    public void cleanUp() {
        socialProfileRepository.deleteAll();
    }

    @Test
    public void shouldSaveProfileWithFriends() {
        // Given
        String userId = "user_XYZ";
        SocialProfile profile = new SocialProfile(userId);
        profile.addFriend("friend_1");
        profile.addFriend("friend_2");

        // When
        socialProfileRepository.save(profile);

        // Then
        Optional<SocialProfile> fetchedOpt = socialProfileRepository.findById(userId);
        
        Assert.assertTrue(fetchedOpt.isPresent());
        SocialProfile fetched = fetchedOpt.get();

        Assert.assertEquals(fetched.getId(), userId);

        Assert.assertEquals(fetched.getFriendIds().size(), 2);
        Assert.assertTrue(fetched.getFriendIds().contains("friend_1"));
        Assert.assertTrue(fetched.getFriendIds().contains("friend_2"));

        Assert.assertEquals(fetched.getFriendCount(), 2);
    }

    @Test
    public void shouldUpdateProfile() {
        // Given
        SocialProfile profile = new SocialProfile("user_ABC");
        profile.addFriend("old_friend");
        socialProfileRepository.save(profile);

        // When
        SocialProfile toUpdate = socialProfileRepository.findById("user_ABC").orElseThrow();
        toUpdate.addFriend("new_friend");
        toUpdate.removeFriend("old_friend");
        socialProfileRepository.save(toUpdate);

        // Then
        SocialProfile updated = socialProfileRepository.findById("user_ABC").orElseThrow();
        Assert.assertEquals(updated.getFriendIds().size(), 1);
        Assert.assertTrue(updated.getFriendIds().contains("new_friend"));
        Assert.assertFalse(updated.getFriendIds().contains("old_friend"));
    }
}