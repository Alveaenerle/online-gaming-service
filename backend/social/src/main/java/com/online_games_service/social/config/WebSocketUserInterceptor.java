package com.online_games_service.social.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

import java.security.Principal;
import java.util.Map;

/**
 * Channel interceptor that sets the user Principal for WebSocket STOMP connections.
 * Uses the userId extracted during handshake and stored in session attributes.
 */
public class WebSocketUserInterceptor implements ChannelInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketUserInterceptor.class);

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            
            if (sessionAttributes != null) {
                String userId = (String) sessionAttributes.get("userId");
                String username = (String) sessionAttributes.get("username");

                if (userId != null) {
                    // Create a Principal with the user ID
                    Principal principal = new WebSocketPrincipal(userId, username);
                    accessor.setUser(principal);
                    logger.info("[WS User] Set principal for user: {} ({})", userId, username);
                } else {
                    logger.debug("[WS User] No userId in session attributes");
                }
            }
        }

        return message;
    }

    /**
     * Simple Principal implementation for WebSocket users.
     */
    public static class WebSocketPrincipal implements Principal {
        private final String userId;
        private final String username;

        public WebSocketPrincipal(String userId, String username) {
            this.userId = userId;
            this.username = username;
        }

        @Override
        public String getName() {
            return userId;
        }

        public String getUsername() {
            return username;
        }

        @Override
        public String toString() {
            return "WebSocketPrincipal{userId='" + userId + "', username='" + username + "'}";
        }
    }
}
