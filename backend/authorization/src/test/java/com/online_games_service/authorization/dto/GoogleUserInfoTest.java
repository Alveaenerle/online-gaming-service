package com.online_games_service.authorization.dto;

import org.testng.Assert;
import org.testng.annotations.Test;

public class GoogleUserInfoTest {

    @Test
    public void shouldCreateWithBuilder() {
        // Given & When
        GoogleUserInfo userInfo = GoogleUserInfo.builder()
            .googleId("google-123")
            .email("test@gmail.com")
            .emailVerified(true)
            .name("John Doe")
            .pictureUrl("https://example.com/photo.jpg")
            .build();

        // Then
        Assert.assertEquals(userInfo.getGoogleId(), "google-123");
        Assert.assertEquals(userInfo.getEmail(), "test@gmail.com");
        Assert.assertTrue(userInfo.isEmailVerified());
        Assert.assertEquals(userInfo.getName(), "John Doe");
        Assert.assertEquals(userInfo.getPictureUrl(), "https://example.com/photo.jpg");
    }

    @Test
    public void shouldCreateWithAllArgsConstructor() {
        // Given & When
        GoogleUserInfo userInfo = new GoogleUserInfo(
            "google-456",
            "user@example.com",
            false,
            "Jane Doe",
            "https://example.com/jane.jpg"
        );

        // Then
        Assert.assertEquals(userInfo.getGoogleId(), "google-456");
        Assert.assertEquals(userInfo.getEmail(), "user@example.com");
        Assert.assertFalse(userInfo.isEmailVerified());
        Assert.assertEquals(userInfo.getName(), "Jane Doe");
        Assert.assertEquals(userInfo.getPictureUrl(), "https://example.com/jane.jpg");
    }

    @Test
    public void shouldCreateWithNoArgsConstructor() {
        // Given
        GoogleUserInfo userInfo = new GoogleUserInfo();

        // When
        userInfo.setGoogleId("google-789");
        userInfo.setEmail("set@test.com");
        userInfo.setEmailVerified(true);
        userInfo.setName("Set Name");
        userInfo.setPictureUrl("https://set.url");

        // Then
        Assert.assertEquals(userInfo.getGoogleId(), "google-789");
        Assert.assertEquals(userInfo.getEmail(), "set@test.com");
        Assert.assertTrue(userInfo.isEmailVerified());
        Assert.assertEquals(userInfo.getName(), "Set Name");
        Assert.assertEquals(userInfo.getPictureUrl(), "https://set.url");
    }

    @Test
    public void shouldSupportEquals() {
        // Given
        GoogleUserInfo userInfo1 = GoogleUserInfo.builder()
            .googleId("google-123")
            .email("test@gmail.com")
            .build();
        
        GoogleUserInfo userInfo2 = GoogleUserInfo.builder()
            .googleId("google-123")
            .email("test@gmail.com")
            .build();
        
        GoogleUserInfo userInfo3 = GoogleUserInfo.builder()
            .googleId("different")
            .email("other@gmail.com")
            .build();

        // Then
        Assert.assertEquals(userInfo1, userInfo2);
        Assert.assertNotEquals(userInfo1, userInfo3);
    }

    @Test
    public void shouldSupportHashCode() {
        // Given
        GoogleUserInfo userInfo1 = GoogleUserInfo.builder()
            .googleId("google-123")
            .email("test@gmail.com")
            .build();
        
        GoogleUserInfo userInfo2 = GoogleUserInfo.builder()
            .googleId("google-123")
            .email("test@gmail.com")
            .build();

        // Then
        Assert.assertEquals(userInfo1.hashCode(), userInfo2.hashCode());
    }

    @Test
    public void shouldSupportToString() {
        // Given
        GoogleUserInfo userInfo = GoogleUserInfo.builder()
            .googleId("google-123")
            .email("test@gmail.com")
            .name("Test User")
            .build();

        // When
        String toString = userInfo.toString();

        // Then
        Assert.assertTrue(toString.contains("google-123"));
        Assert.assertTrue(toString.contains("test@gmail.com"));
        Assert.assertTrue(toString.contains("Test User"));
    }

    @Test
    public void shouldHandleNullValues() {
        // Given & When
        GoogleUserInfo userInfo = GoogleUserInfo.builder()
            .googleId("google-123")
            .email("test@gmail.com")
            .emailVerified(true)
            .name(null)
            .pictureUrl(null)
            .build();

        // Then
        Assert.assertNotNull(userInfo);
        Assert.assertNull(userInfo.getName());
        Assert.assertNull(userInfo.getPictureUrl());
    }
}
