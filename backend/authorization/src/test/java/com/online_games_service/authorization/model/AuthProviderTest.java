package com.online_games_service.authorization.model;

import org.testng.Assert;
import org.testng.annotations.Test;

public class AuthProviderTest {

    @Test
    public void shouldHaveLocalProvider() {
        // When
        AuthProvider local = AuthProvider.LOCAL;

        // Then
        Assert.assertEquals(local.name(), "LOCAL");
    }

    @Test
    public void shouldHaveGoogleProvider() {
        // When
        AuthProvider google = AuthProvider.GOOGLE;

        // Then
        Assert.assertEquals(google.name(), "GOOGLE");
    }

    @Test
    public void shouldHaveExactlyTwoProviders() {
        // When
        AuthProvider[] values = AuthProvider.values();

        // Then
        Assert.assertEquals(values.length, 2);
    }

    @Test
    public void shouldValueOfLocal() {
        // When
        AuthProvider provider = AuthProvider.valueOf("LOCAL");

        // Then
        Assert.assertEquals(provider, AuthProvider.LOCAL);
    }

    @Test
    public void shouldValueOfGoogle() {
        // When
        AuthProvider provider = AuthProvider.valueOf("GOOGLE");

        // Then
        Assert.assertEquals(provider, AuthProvider.GOOGLE);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldThrowExceptionForInvalidValue() {
        // When
        AuthProvider.valueOf("INVALID");
    }
}
