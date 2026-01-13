package com.online_games_service.authorization.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.online_games_service.authorization.dto.GoogleUserInfo;
import com.online_games_service.authorization.exception.InvalidCredentialsException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.security.GeneralSecurityException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class GoogleTokenVerifierServiceTest {

    @Mock
    private GoogleIdTokenVerifier mockVerifier;

    @Mock
    private GoogleIdToken mockIdToken;

    @Mock
    private GoogleIdToken.Payload mockPayload;

    private GoogleTokenVerifierService googleTokenVerifierService;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        googleTokenVerifierService = new GoogleTokenVerifierService();
        googleTokenVerifierService.setVerifier(mockVerifier);
    }

    @Test
    public void shouldVerifyTokenAndReturnUserInfo() throws Exception {
        // Given
        String idTokenString = "valid-google-token";
        String googleId = "google-user-123";
        String email = "test@gmail.com";
        String name = "John Doe";
        String pictureUrl = "https://example.com/photo.jpg";

        when(mockVerifier.verify(idTokenString)).thenReturn(mockIdToken);
        when(mockIdToken.getPayload()).thenReturn(mockPayload);
        when(mockPayload.getSubject()).thenReturn(googleId);
        when(mockPayload.getEmail()).thenReturn(email);
        when(mockPayload.getEmailVerified()).thenReturn(true);
        when(mockPayload.get("name")).thenReturn(name);
        when(mockPayload.get("picture")).thenReturn(pictureUrl);

        // When
        GoogleUserInfo result = googleTokenVerifierService.verifyToken(idTokenString);

        // Then
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getGoogleId(), googleId);
        Assert.assertEquals(result.getEmail(), email);
        Assert.assertEquals(result.getName(), name);
        Assert.assertEquals(result.getPictureUrl(), pictureUrl);
        Assert.assertTrue(result.isEmailVerified());
    }

    @Test
    public void shouldVerifyTokenWithNullNameAndPicture() throws Exception {
        // Given
        String idTokenString = "valid-token";

        when(mockVerifier.verify(idTokenString)).thenReturn(mockIdToken);
        when(mockIdToken.getPayload()).thenReturn(mockPayload);
        when(mockPayload.getSubject()).thenReturn("google-123");
        when(mockPayload.getEmail()).thenReturn("test@gmail.com");
        when(mockPayload.getEmailVerified()).thenReturn(true);
        when(mockPayload.get("name")).thenReturn(null);
        when(mockPayload.get("picture")).thenReturn(null);

        // When
        GoogleUserInfo result = googleTokenVerifierService.verifyToken(idTokenString);

        // Then
        Assert.assertNotNull(result);
        Assert.assertNull(result.getName());
        Assert.assertNull(result.getPictureUrl());
    }

    @Test(expectedExceptions = InvalidCredentialsException.class)
    public void shouldThrowExceptionWhenTokenIsNull() throws Exception {
        // Given
        when(mockVerifier.verify(anyString())).thenReturn(null);

        // When
        googleTokenVerifierService.verifyToken("invalid-token");
    }

    @Test(expectedExceptions = InvalidCredentialsException.class)
    public void shouldThrowExceptionWhenTokenStringIsEmpty() {
        // When
        googleTokenVerifierService.verifyToken("");
    }

    @Test(expectedExceptions = InvalidCredentialsException.class)
    public void shouldThrowExceptionWhenTokenStringIsNull() {
        // When
        googleTokenVerifierService.verifyToken(null);
    }

    @Test(expectedExceptions = InvalidCredentialsException.class)
    public void shouldThrowExceptionWhenTokenStringIsBlank() {
        // When
        googleTokenVerifierService.verifyToken("   ");
    }

    @Test(expectedExceptions = InvalidCredentialsException.class)
    public void shouldThrowExceptionWhenEmailNotVerified() throws Exception {
        // Given
        when(mockVerifier.verify(anyString())).thenReturn(mockIdToken);
        when(mockIdToken.getPayload()).thenReturn(mockPayload);
        when(mockPayload.getSubject()).thenReturn("google-123");
        when(mockPayload.getEmail()).thenReturn("test@gmail.com");
        when(mockPayload.getEmailVerified()).thenReturn(false);

        // When
        googleTokenVerifierService.verifyToken("valid-token");
    }

    @Test(expectedExceptions = InvalidCredentialsException.class)
    public void shouldThrowExceptionOnSecurityException() throws Exception {
        // Given
        when(mockVerifier.verify(anyString())).thenThrow(new GeneralSecurityException("Security error"));

        // When
        googleTokenVerifierService.verifyToken("malicious-token");
    }

    @Test(expectedExceptions = InvalidCredentialsException.class)
    public void shouldThrowExceptionOnIOException() throws Exception {
        // Given
        when(mockVerifier.verify(anyString())).thenThrow(new IOException("Network error"));

        // When
        googleTokenVerifierService.verifyToken("network-error-token");
    }

    @Test(expectedExceptions = InvalidCredentialsException.class)
    public void shouldThrowExceptionOnIllegalArgumentException() throws Exception {
        // Given
        when(mockVerifier.verify(anyString())).thenThrow(new IllegalArgumentException("Invalid format"));

        // When
        googleTokenVerifierService.verifyToken("bad-format-token");
    }

    @Test(expectedExceptions = InvalidCredentialsException.class)
    public void shouldThrowExceptionWhenVerifierNotConfigured() {
        // Given
        GoogleTokenVerifierService unconfiguredService = new GoogleTokenVerifierService();
        // Verifier is null by default when not configured

        // When
        unconfiguredService.verifyToken("any-token");
    }

    @Test
    public void shouldReturnTrueWhenConfigured() {
        // Given - verifier is set in setup

        // When
        boolean result = googleTokenVerifierService.isConfigured();

        // Then
        Assert.assertTrue(result);
    }

    @Test
    public void shouldReturnFalseWhenNotConfigured() {
        // Given
        GoogleTokenVerifierService unconfiguredService = new GoogleTokenVerifierService();

        // When
        boolean result = unconfiguredService.isConfigured();

        // Then
        Assert.assertFalse(result);
    }
}
