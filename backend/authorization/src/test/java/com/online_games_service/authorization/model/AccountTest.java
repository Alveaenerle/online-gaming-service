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

    @Test
    public void shouldCreateLocalAccountWithDefaultAuthProvider() {
        // Given & When
        Account account = new Account("test@example.com", "hash", "user1", "username");

        // Then
        Assert.assertEquals(account.getAuthProvider(), AuthProvider.LOCAL);
        Assert.assertFalse(account.isOAuthAccount());
        Assert.assertNull(account.getGoogleId());
    }

    @Test
    public void shouldCreateOAuthAccountWithGoogleProvider() {
        // Given & When
        Account account = new Account(
            "oauth@gmail.com",
            "user-id-123",
            "oauthUser",
            AuthProvider.GOOGLE,
            "google-123",
            "https://example.com/photo.jpg"
        );

        // Then
        Assert.assertEquals(account.getEmail(), "oauth@gmail.com");
        Assert.assertEquals(account.getUserId(), "user-id-123");
        Assert.assertEquals(account.getUsername(), "oauthUser");
        Assert.assertEquals(account.getAuthProvider(), AuthProvider.GOOGLE);
        Assert.assertEquals(account.getGoogleId(), "google-123");
        Assert.assertEquals(account.getPictureUrl(), "https://example.com/photo.jpg");
        Assert.assertNull(account.getPasswordHash());
    }

    @Test
    public void shouldIdentifyOAuthAccount() {
        // Given
        Account oauthAccount = new Account(
            "oauth@gmail.com",
            "user-id",
            "user",
            AuthProvider.GOOGLE,
            "google-123",
            null
        );

        // Then
        Assert.assertTrue(oauthAccount.isOAuthAccount());
    }

    @Test
    public void shouldIdentifyLocalAccount() {
        // Given
        Account localAccount = new Account("local@example.com", "hash", "user-id", "user");

        // Then
        Assert.assertFalse(localAccount.isOAuthAccount());
    }

    @Test
    public void shouldSetAndGetGoogleId() {
        // Given
        Account account = new Account();

        // When
        account.setGoogleId("google-456");

        // Then
        Assert.assertEquals(account.getGoogleId(), "google-456");
    }

    @Test
    public void shouldSetAndGetPictureUrl() {
        // Given
        Account account = new Account();

        // When
        account.setPictureUrl("https://new.url/photo.jpg");

        // Then
        Assert.assertEquals(account.getPictureUrl(), "https://new.url/photo.jpg");
    }

    @Test
    public void shouldSetAndGetAuthProvider() {
        // Given
        Account account = new Account();

        // When
        account.setAuthProvider(AuthProvider.GOOGLE);

        // Then
        Assert.assertEquals(account.getAuthProvider(), AuthProvider.GOOGLE);
    }

    @Test
    public void shouldHaveDefaultAuthProviderAsLocal() {
        // Given & When
        Account account = new Account();

        // Then
        Assert.assertEquals(account.getAuthProvider(), AuthProvider.LOCAL);
    }

    @Test
    public void shouldAllowNullPasswordHashForOAuthAccounts() {
        // Given
        Account account = new Account(
            "oauth@gmail.com",
            "user-id",
            "user",
            AuthProvider.GOOGLE,
            "google-123",
            null
        );

        // Then
        Assert.assertNull(account.getPasswordHash());
    }
}
