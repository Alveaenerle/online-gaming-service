package com.online_games_service.social.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;

public class WebSocketUserInterceptorTest {

    private WebSocketUserInterceptor interceptor;

    @BeforeMethod
    public void setUp() {
        interceptor = new WebSocketUserInterceptor();
    }

    @Test
    public void preSend_ShouldSetUserPrincipal_WhenConnectCommandAndAttributesExist() {
        // Given
        MessageChannel channel = mock(MessageChannel.class);
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        Map<String, Object> sessionAttributes = new HashMap<>();
        sessionAttributes.put("userId", "user123");
        sessionAttributes.put("username", "testUser");
        accessor.setSessionAttributes(sessionAttributes);
        accessor.setLeaveMutable(true); // Allow headers to be modified
        
        MessageHeaders headers = accessor.getMessageHeaders();
        
        // Use a robust custom Message implementation that exposes the mutable headers
        Message<byte[]> message = new Message<byte[]>() {
            @Override
            public byte[] getPayload() {
                return new byte[0];
            }

            @Override
            public MessageHeaders getHeaders() {
                return headers;
            }
            
            @Override
            public String toString() {
                return "CustomMockMessage";
            }
        };

        // When
        Message<?> result = interceptor.preSend(message, channel);

        // Then
        // We need to check if the header was actually updated.
        // Since we kept it mutable and linked, checking the result's accessor or even the original accessor might work.
        // But interceptor might not return a NEW message, it returns the same message (in current implementation).
        
        StompHeaderAccessor resultAccessor = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        Assert.assertNotNull(resultAccessor);
        Principal user = resultAccessor.getUser();
        
        Assert.assertNotNull(user);
        Assert.assertTrue(user instanceof WebSocketUserInterceptor.WebSocketPrincipal);
        Assert.assertEquals(user.getName(), "user123");
        Assert.assertEquals(((WebSocketUserInterceptor.WebSocketPrincipal) user).getUsername(), "testUser");
    }

    @Test
    public void preSend_ShouldNotSetUser_WhenNotConnectCommand() {
        // Given
        MessageChannel channel = mock(MessageChannel.class);
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        Map<String, Object> sessionAttributes = new HashMap<>();
        sessionAttributes.put("userId", "user123");
        accessor.setSessionAttributes(sessionAttributes);
        accessor.setLeaveMutable(true);
        
        MessageHeaders headers = accessor.getMessageHeaders();
        Message<byte[]> message = new Message<>() {
             public byte[] getPayload() { return new byte[0]; }
             public MessageHeaders getHeaders() { return headers; }
        };

        // When
        Message<?> result = interceptor.preSend(message, channel);

        // Then
        StompHeaderAccessor resultAccessor = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        Assert.assertNull(resultAccessor.getUser());
    }

    @Test
    public void preSend_ShouldNotSetUser_WhenAttributesNull() {
        // Given
        MessageChannel channel = mock(MessageChannel.class);
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setSessionAttributes(null);
        accessor.setLeaveMutable(true);
        
        MessageHeaders headers = accessor.getMessageHeaders();
        Message<byte[]> message = new Message<>() {
             public byte[] getPayload() { return new byte[0]; }
             public MessageHeaders getHeaders() { return headers; }
        };

        // When
        Message<?> result = interceptor.preSend(message, channel);

        // Then
        StompHeaderAccessor resultAccessor = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        Assert.assertNull(resultAccessor.getUser());
    }

    @Test
    public void preSend_ShouldNotSetUser_WhenUserIdMissing() {
        // Given
        MessageChannel channel = mock(MessageChannel.class);
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        Map<String, Object> sessionAttributes = new HashMap<>();
        sessionAttributes.put("username", "testUser"); // No userId
        accessor.setSessionAttributes(sessionAttributes);
        accessor.setLeaveMutable(true);
        
        MessageHeaders headers = accessor.getMessageHeaders();
        Message<byte[]> message = new Message<>() {
             public byte[] getPayload() { return new byte[0]; }
             public MessageHeaders getHeaders() { return headers; }
        };

        // When
        Message<?> result = interceptor.preSend(message, channel);

        // Then
        StompHeaderAccessor resultAccessor = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        Assert.assertNull(resultAccessor.getUser());
    }
}
