package com.online_games_service.authorization.service;

import com.online_games_service.authorization.model.User;
import com.online_games_service.authorization.repository.redis.SessionRedisRepository;
import jakarta.servlet.http.Cookie;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class SessionServiceTest {

    @Mock
    private SessionRedisRepository sessionRepository;

    private SessionService sessionService;

    private final String TEST_COOKIE_NAME = "test_session";
    private final long TEST_TIMEOUT = 3600L;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        sessionService = new SessionService(sessionRepository);
        
        ReflectionTestUtils.setField(sessionService, "cookieName", TEST_COOKIE_NAME);
        ReflectionTestUtils.setField(sessionService, "sessionTimeout", TEST_TIMEOUT);
    }

    @Test
    public void shouldCreateSessionCookieAndSaveToRedis() {
        // Given
        User user = new User("user123", "testUser", false);

        // When
        ResponseCookie cookie = sessionService.createSessionCookie(user);

        // Then
        ArgumentCaptor<String> sessionIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(sessionRepository).saveSession(sessionIdCaptor.capture(), eq(user));
        
        String capturedSessionId = sessionIdCaptor.getValue();
        Assert.assertNotNull(capturedSessionId);

        Assert.assertNotNull(cookie);
        Assert.assertEquals(cookie.getName(), TEST_COOKIE_NAME);
        Assert.assertEquals(cookie.getValue(), capturedSessionId);
        Assert.assertEquals(cookie.getMaxAge(), Duration.ofSeconds(TEST_TIMEOUT));
        Assert.assertEquals(cookie.getPath(), "/");
        Assert.assertTrue(cookie.isHttpOnly());
        Assert.assertEquals(cookie.getSameSite(), "Lax");
    }

    @Test
    public void shouldGetUserFromCookieWhenSessionExists() {
        // Given
        String sessionId = "valid-session-id";
        User expectedUser = new User("user123", "testUser", false);
        
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(TEST_COOKIE_NAME, sessionId));

        when(sessionRepository.findUserBySessionId(sessionId)).thenReturn(Optional.of(expectedUser));

        // When
        User result = sessionService.getUserFromCookie(request);

        // Then
        Assert.assertNotNull(result, "User should not be null when session exists");
        Assert.assertEquals(result.getId(), expectedUser.getId());
        verify(sessionRepository).findUserBySessionId(sessionId);
    }

    @Test
    public void shouldReturnNullWhenCookieIsMissing() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();

        // When
        User result = sessionService.getUserFromCookie(request);

        // Then
        Assert.assertNull(result);
        verify(sessionRepository, never()).findUserBySessionId(any());
    }

    @Test
    public void shouldReturnNullWhenSessionIdIsInvalidOrExpired() {
        // Given
        String invalidSessionId = "expired-id";
        
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(TEST_COOKIE_NAME, invalidSessionId));

        when(sessionRepository.findUserBySessionId(invalidSessionId)).thenReturn(Optional.empty());

        // When
        User result = sessionService.getUserFromCookie(request);

        // Then
        Assert.assertNull(result);
        verify(sessionRepository).findUserBySessionId(invalidSessionId);
    }

    @Test
    public void shouldGetCleanSessionCookie() {
        // When
        ResponseCookie cookie = sessionService.getCleanSessionCookie();

        // Then
        Assert.assertEquals(cookie.getName(), TEST_COOKIE_NAME);
        Assert.assertEquals(cookie.getValue(), "");
        Assert.assertEquals(cookie.getMaxAge(), Duration.ZERO);
        Assert.assertEquals(cookie.getPath(), "/");
    }

    @Test
    public void shouldDeleteSessionWhenCookieExists() {
        // Given
        String sessionId = "session-to-delete";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(TEST_COOKIE_NAME, sessionId));

        // When
        sessionService.deleteSession(request);

        // Then
        verify(sessionRepository).deleteSession(sessionId);
    }

    @Test
    public void shouldNotDeleteSessionWhenCookieIsMissing() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        // Brak ciasteczek

        // When
        sessionService.deleteSession(request);

        // Then
        verify(sessionRepository, never()).deleteSession(any());
    }
    
    @Test
    public void shouldNotDeleteSessionWhenCookieValueIsNull() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        Cookie emptyCookie = new Cookie(TEST_COOKIE_NAME, null);
        request.setCookies(emptyCookie);

        // When
        sessionService.deleteSession(request);

        // Then
        verify(sessionRepository, never()).deleteSession(any());
    }
}