package com.online_games_service.authorization.dto;

import org.testng.Assert;
import org.testng.annotations.Test;

public class RegisterRequestTest {

    @Test
    public void testRegisterRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("user");
        request.setEmail("test@example.com");
        request.setPassword("password");

        Assert.assertEquals(request.getUsername(), "user");
        Assert.assertEquals(request.getEmail(), "test@example.com");
        Assert.assertEquals(request.getPassword(), "password");

        RegisterRequest request2 = new RegisterRequest("user", "test@example.com", "password");
        Assert.assertEquals(request2.getUsername(), "user");
        Assert.assertEquals(request2.getEmail(), "test@example.com");
        Assert.assertEquals(request2.getPassword(), "password");
        
        Assert.assertEquals(request, request2);
        Assert.assertEquals(request.hashCode(), request2.hashCode());
        Assert.assertEquals(request.toString(), request2.toString());
    }
}
