package com.online_games_service.tests;

import org.testng.Assert;
import org.testng.annotations.Test;

public class GreetingResponseTest {

    @Test
    public void shouldCalculateLengthFromMessageInConstructor() {
        // when
        HelloController.GreetingResponse response =
                new HelloController.GreetingResponse("Hello Player");

        // then
        Assert.assertEquals(response.getMessage(), "Hello Player");
        Assert.assertEquals(response.getLength(), "Hello Player".length());
    }

    @Test
    public void shouldRecalculateLengthWhenMessageChanges() {
        // given
        HelloController.GreetingResponse response =
                new HelloController.GreetingResponse();
        response.setMessage("Hi");

        // then
        Assert.assertEquals(response.getMessage(), "Hi");
        Assert.assertEquals(response.getLength(), 2);

        // when
        response.setMessage("Hello There");

        // then
        Assert.assertEquals(response.getMessage(), "Hello There");
        Assert.assertEquals(response.getLength(), "Hello There".length());
    }

    @Test
    public void shouldReturnZeroLengthWhenMessageIsNull() {
        // given
        HelloController.GreetingResponse response =
                new HelloController.GreetingResponse();
        response.setMessage(null);

        // then
        Assert.assertEquals(response.getLength(), 0);
    }
}
