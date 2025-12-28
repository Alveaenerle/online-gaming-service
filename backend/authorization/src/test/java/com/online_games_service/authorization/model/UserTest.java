package com.online_games_service.authorization.model;

import org.testng.Assert;
import org.testng.annotations.Test;

public class UserTest {

    @Test
    public void testUser() {
        User user = new User("1", "username", false);
        Assert.assertEquals(user.getId(), "1");
        Assert.assertEquals(user.getUsername(), "username");
        Assert.assertFalse(user.isGuest());

        User user2 = new User();
        user2.setId("2");
        user2.setUsername("guest");
        user2.setGuest(true);

        Assert.assertEquals(user2.getId(), "2");
        Assert.assertEquals(user2.getUsername(), "guest");
        Assert.assertTrue(user2.isGuest());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testUserNullId() {
        new User(null, "username", false);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testUserEmptyId() {
        new User("", "username", false);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testUserNullUsername() {
        new User("1", null, false);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testUserEmptyUsername() {
        new User("1", "", false);
    }
}
