package com.online_games_service.social.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

/**
 * Listener for WebSocket session events.
 * Handles user connection and disconnection to manage presence status.
 */
@Component
public class WebSocketEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final PresenceService presenceService;
    private final FriendNotificationService friendNotificationService;

    public WebSocketEventListener(
            PresenceService presenceService,
            FriendNotificationService friendNotificationService) {
        this.presenceService = presenceService;
        this.friendNotificationService = friendNotificationService;
    }

    /**
     * Handles WebSocket connection events.
     * Sets the user as online in Redis and notifies their friends.
     */
    @EventListener
    public void handleWebSocketConnect(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();

        if (principal != null) {
            String userId = principal.getName();
            String sessionId = accessor.getSessionId();
            
            logger.info("User {} connected via WebSocket (session: {})", userId, sessionId);
            
            // Set user as online
            presenceService.setUserOnline(userId);
            
            // Notify friends about the user coming online
            friendNotificationService.notifyFriendsUserOnline(userId);
        } else {
            logger.debug("WebSocket connection without authenticated principal (session: {})", 
                    accessor.getSessionId());
        }
    }

    /**
     * Handles WebSocket disconnection events.
     * For graceful disconnects, immediately removes user from online status.
     * For ungraceful disconnects (network loss), Redis TTL expiration will handle it.
     */
    @EventListener
    public void handleWebSocketDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();

        if (principal != null) {
            String userId = principal.getName();
            String sessionId = accessor.getSessionId();
            
            logger.info("User {} disconnected from WebSocket (session: {})", userId, sessionId);
            
            // Remove user from online status immediately on graceful disconnect
            presenceService.removeUserOnline(userId);
            
            // Notify friends about the user going offline
            friendNotificationService.notifyFriendsUserOffline(userId);
        } else {
            logger.debug("WebSocket disconnection without authenticated principal (session: {})", 
                    accessor.getSessionId());
        }
    }
}
