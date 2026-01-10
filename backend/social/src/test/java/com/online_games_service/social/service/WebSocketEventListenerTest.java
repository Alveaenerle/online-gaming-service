package com.online_games_service.social.service;

import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.MessageHeaders;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

public class WebSocketEventListenerTest {

    private WebSocketEventListener eventListener;
    private PresenceService presenceService;
    private FriendNotificationService friendNotificationService;

    @BeforeMethod
    public void setUp() {
        presenceService = mock(PresenceService.class);
        friendNotificationService = mock(FriendNotificationService.class);
        eventListener = new WebSocketEventListener(presenceService, friendNotificationService);
    }

    @Test
    public void shouldSetUserOnlineAndNotifyFriendsOnConnect() {
        // Given
        String userId = "connected_user";
        Principal principal = () -> userId;
        
        Map<String, Object> headers = new HashMap<>();
        headers.put("simpSessionId", "session123");
        headers.put("simpUser", principal);
        
        Message<byte[]> message = new GenericMessage<>(new byte[0], new MessageHeaders(headers));
        SessionConnectedEvent event = new SessionConnectedEvent(this, message, principal);

        // When
        eventListener.handleWebSocketConnect(event);

        // Then
        verify(presenceService).setUserOnline(userId);
        verify(friendNotificationService).notifyFriendsUserOnline(userId);
    }

    @Test
    public void shouldRemoveUserOnlineAndNotifyFriendsOnDisconnect() {
        // Given
        String userId = "disconnected_user";
        Principal principal = () -> userId;
        
        Map<String, Object> headers = new HashMap<>();
        headers.put("simpSessionId", "session456");
        headers.put("simpUser", principal);
        
        Message<byte[]> message = new GenericMessage<>(new byte[0], new MessageHeaders(headers));
        SessionDisconnectEvent event = new SessionDisconnectEvent(this, message, "session456", null);

        // When
        eventListener.handleWebSocketDisconnect(event);

        // Then
        verify(presenceService).removeUserOnline(userId);
        verify(friendNotificationService).notifyFriendsUserOffline(userId);
    }

    @Test
    public void shouldNotProcessConnectWithoutPrincipal() {
        // Given
        Map<String, Object> headers = new HashMap<>();
        headers.put("simpSessionId", "session789");
        // No principal
        
        Message<byte[]> message = new GenericMessage<>(new byte[0], new MessageHeaders(headers));
        SessionConnectedEvent event = new SessionConnectedEvent(this, message, null);

        // When
        eventListener.handleWebSocketConnect(event);

        // Then
        verify(presenceService, never()).setUserOnline(anyString());
        verify(friendNotificationService, never()).notifyFriendsUserOnline(anyString());
    }

    @Test
    public void shouldNotProcessDisconnectWithoutPrincipal() {
        // Given
        Map<String, Object> headers = new HashMap<>();
        headers.put("simpSessionId", "session101");
        // No principal
        
        Message<byte[]> message = new GenericMessage<>(new byte[0], new MessageHeaders(headers));
        SessionDisconnectEvent event = new SessionDisconnectEvent(this, message, "session101", null);

        // When
        eventListener.handleWebSocketDisconnect(event);

        // Then
        verify(presenceService, never()).removeUserOnline(anyString());
        verify(friendNotificationService, never()).notifyFriendsUserOffline(anyString());
    }
}
