package com.online_games_service.authorization.dto;

import org.testng.Assert;
import org.testng.annotations.Test;

public class LoginRequestTest {

    @Test
    public void testLoginRequest() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password");

        Assert.assertEquals(request.getEmail(), "test@example.com");
        Assert.assertEquals(request.getPassword(), "password");

        LoginRequest request2 = new LoginRequest("test@example.com", "password");
        Assert.assertEquals(request2.getEmail(), "test@example.com");
        Assert.assertEquals(request2.getPassword(), "password");
        
        Assert.assertEquals(request, request2);
        Assert.assertEquals(request.hashCode(), request2.hashCode());
        Assert.assertEquals(request.toString(), request2.toString());
    }
}
