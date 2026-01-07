package com.online_games_service.common.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class SessionUserFilter extends OncePerRequestFilter {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String COOKIE_NAME = "ogs_session";
    private static final String REDIS_PREFIX = "auth:session:";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (request.getCookies() != null) {
            Arrays.stream(request.getCookies())
                    .filter(c -> COOKIE_NAME.equals(c.getName()))
                    .forEach(c -> log.info("[Filter] Cookie found: name={}, value={}", c.getName(), c.getValue()));
        }

        String sessionId = getSessionIdFromCookies(request);

        if (sessionId != null) {
            log.info("[Filter] Found session ID in cookie: {}", sessionId);
            try {
                String redisKey = REDIS_PREFIX + sessionId;

                log.info("[Filter] Looking for key in Redis: {}", redisKey);
                Object sessionData = redisTemplate.opsForValue().get(redisKey);

                if (sessionData == null) {
                    log.warn("[Filter] Key '{}' not found in Redis (expired or wrong prefix).", redisKey);
                } else {
                    log.info("[Filter] Redis returned data type: {}", sessionData.getClass().getName());

                    String username = extractField(sessionData, "username");
                    String userId = extractField(sessionData, "id");

                    if (userId != null) {
                        request.setAttribute("userId", userId);
                        log.info("[Filter] SUCCESS! User ID extracted: {}", userId);
                    } else {
                        log.warn("[Filter] Could not extract 'id' field from session data.");
                    }

                    if (username != null) {
                        request.setAttribute("username", username);
                        log.info("[Filter] SUCCESS! Username extracted: {}", username);
                    } else {
                        log.warn("[Filter] Could not extract 'username' field from session data.");
                    }
                }
            } catch (Exception e) {
                log.error("[Filter] Critical Redis error: ", e);
            }
        } else {
            log.info("[Filter] required cookie '{}' NOT found.", COOKIE_NAME);
        }

        filterChain.doFilter(request, response);
    }

    private String extractField(Object sessionData, String key) {
        try {
            if (sessionData instanceof Map) {
                Object value = ((Map<?, ?>) sessionData).get(key);
                return value != null ? value.toString() : null;
            }

            // Convert the sessionData object to a Map to safely access fields
            // without requiring the concrete User class to be present on the classpath.
            Map<String, Object> map = objectMapper.convertValue(sessionData, Map.class);
            Object value = map.get(key);
            return value != null ? value.toString() : null;

        } catch (Exception e) {
            log.error("[Filter] Parsing error for type {}: {}", sessionData.getClass().getName(), e.getMessage());
            return null;
        }
    }

    private String getSessionIdFromCookies(HttpServletRequest request) {
        if (request.getCookies() == null)
            return null;

        return Arrays.stream(request.getCookies())
                .filter(cookie -> COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}