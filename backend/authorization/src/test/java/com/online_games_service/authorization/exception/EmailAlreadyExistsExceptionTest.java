package com.online_games_service.authorization.exception;

import org.testng.Assert;
import org.testng.annotations.Test;

public class EmailAlreadyExistsExceptionTest {

    @Test
    public void testException() {
        String message = "Email exists";
        EmailAlreadyExistsException exception = new EmailAlreadyExistsException(message);
        Assert.assertEquals(exception.getMessage(), message);
    }
}
