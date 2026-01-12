package com.online_games_service.menu.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class WebSocketAuthInterceptorTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ServletServerHttpRequest servletServerHttpRequest;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private ServerHttpResponse response;

    @Mock
    private WebSocketHandler wsHandler;

    private WebSocketAuthInterceptor interceptor;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(servletServerHttpRequest.getServletRequest()).thenReturn(httpServletRequest);
        interceptor = new WebSocketAuthInterceptor(redisTemplate);
    }

    @Test
    public void beforeHandshake_shouldExtractUserFromSession_whenValidSession() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        Cookie sessionCookie = new Cookie("ogs_session", "session-123");
        when(httpServletRequest.getCookies()).thenReturn(new Cookie[]{sessionCookie});

        String sessionJson = "{\"id\":\"user-456\",\"username\":\"TestUser\",\"email\":\"test@example.com\"}";
        when(valueOperations.get("auth:session:session-123")).thenReturn(sessionJson);

        boolean result = interceptor.beforeHandshake(servletServerHttpRequest, response, wsHandler, attributes);

        assertTrue(result);
        assertEquals(attributes.get("userId"), "user-456");
        assertEquals(attributes.get("username"), "TestUser");
    }

    @Test
    public void beforeHandshake_shouldAllowConnection_whenNoSessionCookie() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        when(httpServletRequest.getCookies()).thenReturn(null);

        boolean result = interceptor.beforeHandshake(servletServerHttpRequest, response, wsHandler, attributes);

        assertTrue(result);
        assertFalse(attributes.containsKey("userId"));
    }

    @Test
    public void beforeHandshake_shouldAllowConnection_whenNoCookies() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        when(httpServletRequest.getCookies()).thenReturn(new Cookie[]{});

        boolean result = interceptor.beforeHandshake(servletServerHttpRequest, response, wsHandler, attributes);

        assertTrue(result);
        assertFalse(attributes.containsKey("userId"));
    }

    @Test
    public void beforeHandshake_shouldAllowConnection_whenSessionNotInRedis() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        Cookie sessionCookie = new Cookie("ogs_session", "nonexistent-session");
        when(httpServletRequest.getCookies()).thenReturn(new Cookie[]{sessionCookie});
        when(valueOperations.get("auth:session:nonexistent-session")).thenReturn(null);

        boolean result = interceptor.beforeHandshake(servletServerHttpRequest, response, wsHandler, attributes);

        assertTrue(result);
        assertFalse(attributes.containsKey("userId"));
    }

    @Test
    public void beforeHandshake_shouldAllowConnection_whenRedisThrowsException() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        Cookie sessionCookie = new Cookie("ogs_session", "session-123");
        when(httpServletRequest.getCookies()).thenReturn(new Cookie[]{sessionCookie});
        when(valueOperations.get("auth:session:session-123")).thenThrow(new RuntimeException("Redis down"));

        boolean result = interceptor.beforeHandshake(servletServerHttpRequest, response, wsHandler, attributes);

        assertTrue(result);
        assertFalse(attributes.containsKey("userId"));
    }

    @Test
    public void beforeHandshake_shouldAllowConnection_whenSessionHasNoUserId() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        Cookie sessionCookie = new Cookie("ogs_session", "session-123");
        when(httpServletRequest.getCookies()).thenReturn(new Cookie[]{sessionCookie});

        String sessionJson = "{\"email\":\"test@example.com\"}";
        when(valueOperations.get("auth:session:session-123")).thenReturn(sessionJson);

        boolean result = interceptor.beforeHandshake(servletServerHttpRequest, response, wsHandler, attributes);

        assertTrue(result);
        assertFalse(attributes.containsKey("userId"));
    }

    @Test
    public void beforeHandshake_shouldIgnoreOtherCookies() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        Cookie otherCookie = new Cookie("other_cookie", "value");
        Cookie sessionCookie = new Cookie("ogs_session", "session-123");
        when(httpServletRequest.getCookies()).thenReturn(new Cookie[]{otherCookie, sessionCookie});

        String sessionJson = "{\"id\":\"user-456\",\"username\":\"TestUser\"}";
        when(valueOperations.get("auth:session:session-123")).thenReturn(sessionJson);

        boolean result = interceptor.beforeHandshake(servletServerHttpRequest, response, wsHandler, attributes);

        assertTrue(result);
        assertEquals(attributes.get("userId"), "user-456");
    }

    @Test
    public void beforeHandshake_shouldAllowConnection_whenNotServletRequest() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        ServerHttpRequest genericRequest = mock(ServerHttpRequest.class);

        boolean result = interceptor.beforeHandshake(genericRequest, response, wsHandler, attributes);

        assertTrue(result);
        assertFalse(attributes.containsKey("userId"));
    }

    @Test
    public void beforeHandshake_shouldHandleInvalidJson() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        Cookie sessionCookie = new Cookie("ogs_session", "session-123");
        when(httpServletRequest.getCookies()).thenReturn(new Cookie[]{sessionCookie});
        when(valueOperations.get("auth:session:session-123")).thenReturn("not valid json");

        boolean result = interceptor.beforeHandshake(servletServerHttpRequest, response, wsHandler, attributes);

        assertTrue(result);
        assertFalse(attributes.containsKey("userId"));
    }

    @Test
    public void beforeHandshake_shouldHandleNullUsernameGracefully() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        Cookie sessionCookie = new Cookie("ogs_session", "session-123");
        when(httpServletRequest.getCookies()).thenReturn(new Cookie[]{sessionCookie});

        String sessionJson = "{\"id\":\"user-456\"}";
        when(valueOperations.get("auth:session:session-123")).thenReturn(sessionJson);

        boolean result = interceptor.beforeHandshake(servletServerHttpRequest, response, wsHandler, attributes);

        assertTrue(result);
        assertEquals(attributes.get("userId"), "user-456");
        assertNull(attributes.get("username"));
    }

    @Test
    public void afterHandshake_shouldDoNothing() {
        // This is a no-op method, just verify it doesn't throw
        interceptor.afterHandshake(servletServerHttpRequest, response, wsHandler, null);
        interceptor.afterHandshake(servletServerHttpRequest, response, wsHandler, new RuntimeException("test"));
    }
}
