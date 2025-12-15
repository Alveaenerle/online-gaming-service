package com.online_games_service.social.model;

import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.HashSet;
import java.util.Set;

public class SocialProfileTest {

    @Test
    public void shouldInitializeEmpty() {
        // Given
        SocialProfile profile = new SocialProfile("user1");

        // Then
        Assert.assertTrue(profile.getFriendIds().isEmpty());
        Assert.assertEquals(profile.getFriendCount(), 0);
    }

    @Test
    public void shouldKeepCountSyncedWhenAddingFriends() {
        // Given
        SocialProfile profile = new SocialProfile("user1");

        // When
        profile.addFriend("friend1");
        profile.addFriend("friend2");

        // Then
        Assert.assertEquals(profile.getFriendCount(), 2);
        Assert.assertEquals(profile.getFriendIds().size(), 2);
        Assert.assertTrue(profile.getFriendIds().contains("friend1"));
    }

    @Test
    public void shouldIgnoreDuplicateAdds() {
        // Given
        SocialProfile profile = new SocialProfile("user1");
        profile.addFriend("uniqueFriend");

        // When
        profile.addFriend("uniqueFriend");

        // Then
        Assert.assertEquals(profile.getFriendCount(), 1);
        Assert.assertEquals(profile.getFriendIds().size(), 1);
    }

    @Test
    public void shouldKeepCountSyncedWhenRemovingFriends() {
        // Given
        SocialProfile profile = new SocialProfile("user1");
        profile.addFriend("friendA");
        profile.addFriend("friendB");

        // When
        profile.removeFriend("friendA");

        // Then
        Assert.assertEquals(profile.getFriendCount(), 1);
        Assert.assertFalse(profile.getFriendIds().contains("friendA"));
        Assert.assertTrue(profile.getFriendIds().contains("friendB"));
    }

    @Test
    public void shouldHandleRemovingNonExistentFriend() {
        // Given
        SocialProfile profile = new SocialProfile("user1");
        profile.addFriend("existingFriend");
        int initialCount = profile.getFriendCount();

        // When
        profile.removeFriend("ghostFriend");

        // Then
        Assert.assertEquals(profile.getFriendCount(), initialCount);
        Assert.assertEquals(profile.getFriendIds().size(), 1);
    }
    
    @Test
    public void shouldThrowExceptionOnDirectSetModification() {
        // Given
        SocialProfile profile = new SocialProfile("user1");
        profile.addFriend("friendA");
        
        // When & Then
        Assert.assertThrows(UnsupportedOperationException.class, () -> {
            profile.getFriendIds().add("friendB");
        });
    }

    @Test
    public void shouldUpdateCountWhenSettingNewSet() {
        // Given
        SocialProfile profile = new SocialProfile("user1");
        Set<String> newFriends = new HashSet<>();
        newFriends.add("f1");
        newFriends.add("f2");
        newFriends.add("f3");

        // When
        profile.setFriendIds(newFriends);

        // Then
        Assert.assertEquals(profile.getFriendCount(), 3);
        Assert.assertEquals(profile.getFriendIds().size(), 3);
    }
}