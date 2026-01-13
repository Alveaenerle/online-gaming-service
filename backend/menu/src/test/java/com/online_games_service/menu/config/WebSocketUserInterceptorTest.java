package com.online_games_service.menu.config;

import com.online_games_service.menu.config.WebSocketUserInterceptor.WebSocketPrincipal;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.*;

public class WebSocketUserInterceptorTest {

    @Mock
    private MessageChannel channel;

    private WebSocketUserInterceptor interceptor;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        interceptor = new WebSocketUserInterceptor();
    }

    @Test
    public void preSend_shouldSetPrincipal_whenConnectCommandWithUserAttributes() {
        Map<String, Object> sessionAttributes = new HashMap<>();
        sessionAttributes.put("userId", "user-123");
        sessionAttributes.put("username", "TestUser");

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setSessionAttributes(sessionAttributes);
        accessor.setLeaveMutable(true);

        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, channel);

        assertNotNull(result);
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertNotNull(resultAccessor.getUser());
        assertEquals(resultAccessor.getUser().getName(), "user-123");
        assertTrue(resultAccessor.getUser() instanceof WebSocketPrincipal);
        assertEquals(((WebSocketPrincipal) resultAccessor.getUser()).getUsername(), "TestUser");
    }

    @Test
    public void preSend_shouldNotSetPrincipal_whenNoUserId() {
        Map<String, Object> sessionAttributes = new HashMap<>();
        sessionAttributes.put("username", "TestUser");

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setSessionAttributes(sessionAttributes);
        accessor.setLeaveMutable(true);

        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, channel);

        assertNotNull(result);
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertNull(resultAccessor.getUser());
    }

    @Test
    public void preSend_shouldNotSetPrincipal_whenNotConnectCommand() {
        Map<String, Object> sessionAttributes = new HashMap<>();
        sessionAttributes.put("userId", "user-123");
        sessionAttributes.put("username", "TestUser");

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setSessionAttributes(sessionAttributes);

        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, channel);

        assertNotNull(result);
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        // Principal should not be set for non-CONNECT commands
        assertNull(resultAccessor.getUser());
    }

    @Test
    public void preSend_shouldNotSetPrincipal_whenNoSessionAttributes() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setSessionAttributes(null);
        accessor.setLeaveMutable(true);

        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, channel);

        assertNotNull(result);
    }

    @Test
    public void preSend_shouldReturnMessage_whenNoStompHeaderAccessor() {
        // Create a simple message without STOMP headers
        Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).build();

        Message<?> result = interceptor.preSend(message, channel);

        // Should return the message unchanged
        assertNotNull(result);
    }

    // ============= WebSocketPrincipal tests =============

    @Test
    public void webSocketPrincipal_shouldReturnUserId() {
        WebSocketPrincipal principal = new WebSocketPrincipal("user-123", "TestUser");

        assertEquals(principal.getName(), "user-123");
    }

    @Test
    public void webSocketPrincipal_shouldReturnUsername() {
        WebSocketPrincipal principal = new WebSocketPrincipal("user-123", "TestUser");

        assertEquals(principal.getUsername(), "TestUser");
    }

    @Test
    public void webSocketPrincipal_shouldHandleNullUsername() {
        WebSocketPrincipal principal = new WebSocketPrincipal("user-123", null);

        assertEquals(principal.getName(), "user-123");
        assertNull(principal.getUsername());
    }

    @Test
    public void webSocketPrincipal_toStringShouldContainUserInfo() {
        WebSocketPrincipal principal = new WebSocketPrincipal("user-123", "TestUser");

        String str = principal.toString();
        assertTrue(str.contains("user-123"));
        assertTrue(str.contains("TestUser"));
    }
}
