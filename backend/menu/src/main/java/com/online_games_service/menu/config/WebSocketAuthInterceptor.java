package com.online_games_service.menu.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

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

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WebSocketAuthInterceptor(StringRedisTemplate redisTemplate) {
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
                    String sessionJson = redisTemplate.opsForValue().get(redisKey);

                    if (sessionJson != null) {
                        Map<String, Object> sessionData = objectMapper.readValue(sessionJson, Map.class);
                        String userId = extractField(sessionData, "id");
                        String username = extractField(sessionData, "username");

                        if (userId != null) {
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

        // Allow connection but without authentication
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

    private String extractField(Map<String, Object> sessionData, String key) {
        try {
            Object value = sessionData.get(key);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            logger.error("[WS Auth] Error extracting field '{}': {}", key, e.getMessage());
            return null;
        }
    }
}
