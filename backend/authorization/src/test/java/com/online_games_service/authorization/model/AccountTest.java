package com.online_games_service.authorization.model;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.LocalDateTime;

public class AccountTest {

    @Test
    public void testAccount() {
        Account account = new Account();
        account.setId("1");
        account.setEmail("test@example.com");
        account.setPasswordHash("hash");
        account.setUserId("user1");
        account.setUsername("username");
        LocalDateTime now = LocalDateTime.now();
        account.setCreatedAt(now);

        Assert.assertEquals(account.getId(), "1");
        Assert.assertEquals(account.getEmail(), "test@example.com");
        Assert.assertEquals(account.getPasswordHash(), "hash");
        Assert.assertEquals(account.getUserId(), "user1");
        Assert.assertEquals(account.getUsername(), "username");
        Assert.assertEquals(account.getCreatedAt(), now);

        Account account2 = new Account("test@example.com", "hash", "user1", "username");
        Assert.assertEquals(account2.getEmail(), "test@example.com");
        Assert.assertEquals(account2.getPasswordHash(), "hash");
        Assert.assertEquals(account2.getUserId(), "user1");
        Assert.assertEquals(account2.getUsername(), "username");
    }
}
