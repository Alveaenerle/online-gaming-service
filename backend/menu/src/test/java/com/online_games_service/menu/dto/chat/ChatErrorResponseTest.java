package com.online_games_service.menu.dto.chat;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class ChatErrorResponseTest {

    @Test
    public void rateLimited_shouldCreateCorrectResponse() {
        ChatErrorResponse response = ChatErrorResponse.rateLimited(5000L);

        assertEquals(response.getCode(), "RATE_LIMIT");
        assertEquals(response.getMessage(), "You are sending messages too quickly. Please wait.");
        assertEquals(response.getRetryAfter(), Long.valueOf(5000L));
    }

    @Test
    public void notInLobby_shouldCreateCorrectResponse() {
        ChatErrorResponse response = ChatErrorResponse.notInLobby();

        assertEquals(response.getCode(), "NOT_IN_LOBBY");
        assertEquals(response.getMessage(), "You are not in a lobby.");
        assertNull(response.getRetryAfter());
    }

    @Test
    public void invalidMessage_shouldCreateCorrectResponse() {
        ChatErrorResponse response = ChatErrorResponse.invalidMessage("Message cannot be empty");

        assertEquals(response.getCode(), "INVALID_MESSAGE");
        assertEquals(response.getMessage(), "Message cannot be empty");
        assertNull(response.getRetryAfter());
    }

    @Test
    public void builder_shouldCreateResponse() {
        ChatErrorResponse response = ChatErrorResponse.builder()
                .code("CUSTOM_ERROR")
                .message("Custom message")
                .retryAfter(1000L)
                .build();

        assertEquals(response.getCode(), "CUSTOM_ERROR");
        assertEquals(response.getMessage(), "Custom message");
        assertEquals(response.getRetryAfter(), Long.valueOf(1000L));
    }

    @Test
    public void noArgsConstructor_shouldCreateEmptyResponse() {
        ChatErrorResponse response = new ChatErrorResponse();

        assertNull(response.getCode());
        assertNull(response.getMessage());
        assertNull(response.getRetryAfter());
    }

    @Test
    public void allArgsConstructor_shouldCreateResponse() {
        ChatErrorResponse response = new ChatErrorResponse("CODE", "Message", 2000L);

        assertEquals(response.getCode(), "CODE");
        assertEquals(response.getMessage(), "Message");
        assertEquals(response.getRetryAfter(), Long.valueOf(2000L));
    }
}
