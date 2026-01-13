package com.online_games_service.authorization.exception;

import org.testng.Assert;
import org.testng.annotations.Test;

public class OAuthAccountExceptionTest {

    @Test
    public void shouldCreateExceptionWithMessage() {
        // Given
        String message = "OAuth account cannot use password login";

        // When
        OAuthAccountException exception = new OAuthAccountException(message);

        // Then
        Assert.assertEquals(exception.getMessage(), message);
    }

    @Test
    public void shouldBeRuntimeException() {
        // When
        OAuthAccountException exception = new OAuthAccountException("Test");

        // Then
        Assert.assertTrue(exception instanceof RuntimeException);
    }

    @Test(expectedExceptions = OAuthAccountException.class)
    public void shouldBeThrowable() {
        // When & Then
        throw new OAuthAccountException("Test exception");
    }

    @Test
    public void shouldHandleNullMessage() {
        // When
        OAuthAccountException exception = new OAuthAccountException(null);

        // Then
        Assert.assertNull(exception.getMessage());
    }

    @Test
    public void shouldHandleEmptyMessage() {
        // When
        OAuthAccountException exception = new OAuthAccountException("");

        // Then
        Assert.assertEquals(exception.getMessage(), "");
    }
}
