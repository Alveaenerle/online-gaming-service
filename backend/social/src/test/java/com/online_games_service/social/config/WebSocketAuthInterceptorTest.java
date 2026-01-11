package com.online_games_service.social.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class WebSocketAuthInterceptorTest {

    private WebSocketAuthInterceptor interceptor;
    private RedisTemplate<String, Object> redisTemplate;
    private ValueOperations<String, Object> valueOperations;
    private ObjectMapper objectMapper;

    @BeforeMethod
    public void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        interceptor = new WebSocketAuthInterceptor(redisTemplate);
        objectMapper = new ObjectMapper();
    }

    @Test
    public void beforeHandshake_ShouldAuthenticateUser_WhenSessionIsValid() throws Exception {
        // Given
        ServletServerHttpRequest request = mock(ServletServerHttpRequest.class);
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        WebSocketHandler wsHandler = mock(WebSocketHandler.class);
        Map<String, Object> attributes = new HashMap<>();
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);

        when(request.getServletRequest()).thenReturn(servletRequest);
        Cookie cookie = new Cookie("ogs_session", "session123");
        when(servletRequest.getCookies()).thenReturn(new Cookie[]{cookie});

        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("id", "user1");
        sessionData.put("username", "testUser");
        when(valueOperations.get("auth:session:session123")).thenReturn(sessionData);

        // When
        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        // Then
        Assert.assertTrue(result);
        Assert.assertEquals(attributes.get("userId"), "user1");
        Assert.assertEquals(attributes.get("username"), "testUser");
    }

    @Test
    public void beforeHandshake_ShouldReturnTrueButNotSetAttributes_WhenNoSessionCookie() throws Exception {
        // Given
        ServletServerHttpRequest request = mock(ServletServerHttpRequest.class);
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        WebSocketHandler wsHandler = mock(WebSocketHandler.class);
        Map<String, Object> attributes = new HashMap<>();
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);

        when(request.getServletRequest()).thenReturn(servletRequest);
        when(servletRequest.getCookies()).thenReturn(null);

        // When
        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        // Then
        Assert.assertTrue(result);
        Assert.assertNull(attributes.get("userId"));
    }

    @Test
    public void beforeHandshake_ShouldReturnTrueButNotSetAttributes_WhenSessionNotFoundInRedis() throws Exception {
        // Given
        ServletServerHttpRequest request = mock(ServletServerHttpRequest.class);
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        WebSocketHandler wsHandler = mock(WebSocketHandler.class);
        Map<String, Object> attributes = new HashMap<>();
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);

        when(request.getServletRequest()).thenReturn(servletRequest);
        Cookie cookie = new Cookie("ogs_session", "session123");
        when(servletRequest.getCookies()).thenReturn(new Cookie[]{cookie});

        when(valueOperations.get("auth:session:session123")).thenReturn(null);

        // When
        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        // Then
        Assert.assertTrue(result);
        Assert.assertNull(attributes.get("userId"));
    }

    @Test
    public void beforeHandshake_ShouldAuthenticateUser_WhenSessionDataIsMapObject() throws Exception {
        // Given
        ServletServerHttpRequest request = mock(ServletServerHttpRequest.class);
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        WebSocketHandler wsHandler = mock(WebSocketHandler.class);
        Map<String, Object> attributes = new HashMap<>();
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);

        when(request.getServletRequest()).thenReturn(servletRequest);
        Cookie cookie = new Cookie("ogs_session", "session123");
        when(servletRequest.getCookies()).thenReturn(new Cookie[]{cookie});

        // Simulate session data as a POJO that converts to map via ObjectMapper inside the interceptor
        // Actually the code handles Map directly or convertValue used on object.
        // Let's test the catch-all "object" case if it wasn't a map directly (mocking simple object might be tricky without a class)
        // But since we mock redisTemplate, we can just return a Map which is standard.
        // Let's test with a mock object that is NOT instance of Map
        TestSessionObject sessionObject = new TestSessionObject("user2", "userTwo");
        when(valueOperations.get("auth:session:session123")).thenReturn(sessionObject);

        // When
        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        // Then
        Assert.assertTrue(result);
        // Note: The conversion relies on ObjectMapper.convertValue(pojo, Map.class)
        // Ensure ObjectMapper can serialize TestSessionObject
        Assert.assertEquals(attributes.get("userId"), "user2");
        Assert.assertEquals(attributes.get("username"), "userTwo");
    }

    @Test
    public void beforeHandshake_ShouldHandleRedisExceptionGracefully() throws Exception {
        // Given
        ServletServerHttpRequest request = mock(ServletServerHttpRequest.class);
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        WebSocketHandler wsHandler = mock(WebSocketHandler.class);
        Map<String, Object> attributes = new HashMap<>();
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);

        when(request.getServletRequest()).thenReturn(servletRequest);
        Cookie cookie = new Cookie("ogs_session", "session123");
        when(servletRequest.getCookies()).thenReturn(new Cookie[]{cookie});

        when(valueOperations.get("auth:session:session123")).thenThrow(new RuntimeException("Redis down"));

        // When
        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        // Then
        Assert.assertTrue(result);
        Assert.assertNull(attributes.get("userId"));
    }

    @Test
    public void beforeHandshake_ShouldDoNothing_WhenNotServletRequest() throws Exception {
        // Given
        ServerHttpRequest request = mock(ServerHttpRequest.class); // Not ServletServerHttpRequest
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        WebSocketHandler wsHandler = mock(WebSocketHandler.class);
        Map<String, Object> attributes = new HashMap<>();

        // When
        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        // Then
        Assert.assertTrue(result);
        Assert.assertNull(attributes.get("userId"));
    }

    @Test
    public void afterHandshake_ShouldDoNothing() {
        // Just covering the method
        interceptor.afterHandshake(null, null, null, null);
    }
    
    // Helper class for POJO test
    public static class TestSessionObject {
        public String id;
        public String username;
        
        public TestSessionObject() {}
        public TestSessionObject(String id, String username) {
            this.id = id;
            this.username = username;
        }
    }
}
