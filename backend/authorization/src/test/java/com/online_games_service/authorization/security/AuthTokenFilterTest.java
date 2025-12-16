package com.online_games_service.authorization.security;

import com.online_games_service.authorization.model.User;
import com.online_games_service.authorization.service.SessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AuthTokenFilterTest {

    @Mock
    private SessionService sessionService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private AuthTokenFilter authTokenFilter;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        authTokenFilter = new AuthTokenFilter(sessionService);
        
        SecurityContextHolder.clearContext();
    }

    @AfterMethod
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void shouldAuthenticateUserWhenValidSessionExists() throws ServletException, IOException {
        // Given
        User user = new User("user123", "testUser", false);
        when(sessionService.getUserFromCookie(request)).thenReturn(user);

        // When
        authTokenFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Assert.assertNotNull(authentication, "Authentication object should be present in context");
        Assert.assertEquals(authentication.getPrincipal(), user);
        Assert.assertTrue(authentication.isAuthenticated());

        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void shouldNotAuthenticateWhenSessionIsMissingOrInvalid() throws ServletException, IOException {
        // Given
        when(sessionService.getUserFromCookie(request)).thenReturn(null);

        // When
        authTokenFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Assert.assertNull(authentication, "Context should be empty when no session exists");

        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void shouldHandleExceptionGracefullyAndContinueChain() throws ServletException, IOException {
        // Given
        when(sessionService.getUserFromCookie(any())).thenThrow(new RuntimeException("Redis unavailable"));

        // When
        authTokenFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Assert.assertNull(authentication);

        verify(filterChain).doFilter(request, response);
    }
}