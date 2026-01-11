package com.online_games_service.social.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.security.Principal;
import java.util.Arrays;
import java.util.Map;

/**
 * WebSocket handshake interceptor that authenticates users using the session cookie.
 * Extracts user ID from Redis session and creates a Principal for WebSocket messaging.
 */
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);
    private static final String COOKIE_NAME = "ogs_session";
    private static final String REDIS_PREFIX = "auth:session:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WebSocketAuthInterceptor(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpRequest = servletRequest.getServletRequest();
            String sessionId = getSessionIdFromCookies(httpRequest);

            if (sessionId != null) {
                logger.debug("[WS Auth] Found session ID: {}", sessionId);
                try {
                    String redisKey = REDIS_PREFIX + sessionId;
                    Object sessionData = redisTemplate.opsForValue().get(redisKey);

                    if (sessionData != null) {
                        String userId = extractField(sessionData, "id");
                        String username = extractField(sessionData, "username");

                        if (userId != null) {
                            // Store user info in attributes for later use
                            attributes.put("userId", userId);
                            attributes.put("username", username);
                            logger.info("[WS Auth] User authenticated: userId={}, username={}", userId, username);
                            return true;
                        }
                    }
                    logger.warn("[WS Auth] Session not found in Redis for sessionId: {}", sessionId);
                } catch (Exception e) {
                    logger.error("[WS Auth] Error reading session from Redis: {}", e.getMessage());
                }
            } else {
                logger.debug("[WS Auth] No session cookie found");
            }
        }

        // Allow connection but without authentication (Principal will be null)
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // No-op
    }

    private String getSessionIdFromCookies(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        
        return Arrays.stream(request.getCookies())
                .filter(c -> COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private String extractField(Object sessionData, String key) {
        try {
            if (sessionData instanceof Map) {
                Object value = ((Map<?, ?>) sessionData).get(key);
                return value != null ? value.toString() : null;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.convertValue(sessionData, Map.class);
            Object value = map.get(key);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            logger.error("[WS Auth] Error extracting field '{}': {}", key, e.getMessage());
            return null;
        }
    }
}
