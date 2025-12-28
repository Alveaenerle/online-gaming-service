package com.online_games_service.authorization.exception;

import org.testng.Assert;
import org.testng.annotations.Test;

public class InvalidCredentialsExceptionTest {

    @Test
    public void testException() {
        String message = "Invalid credentials";
        InvalidCredentialsException exception = new InvalidCredentialsException(message);
        Assert.assertEquals(exception.getMessage(), message);
    }
}
