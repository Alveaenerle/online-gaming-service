package com.online_games_service.authorization.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.online_games_service.authorization.dto.LoginRequest;
import com.online_games_service.authorization.dto.RegisterRequest;
import com.online_games_service.authorization.exception.EmailAlreadyExistsException;
import com.online_games_service.authorization.exception.InvalidCredentialsException;
import com.online_games_service.authorization.model.User;
import com.online_games_service.authorization.service.AuthService;
import com.online_games_service.authorization.service.SessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private SessionService sessionService;

    // REGSITER TESTS

    @Test
    void shouldRegisterUserSuccessfully() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest("testuser", "test@email.com", "password123");
        
        // When
        
        // Then
        mockMvc.perform(post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered successfully!"));
        
        verify(authService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenEmailAlreadyExists() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest("testuser", "duplicate@email.com", "password123");
        doThrow(new EmailAlreadyExistsException("Email already taken"))
                .when(authService).register(any(RegisterRequest.class));

        // When & Then
        mockMvc.perform(post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Email already taken"));
    }

    @Test
    void shouldReturnInternalServerErrorOnUnexpectedRegistrationError() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest("testuser", "error@email.com", "password123");
        doThrow(new RuntimeException("DB down"))
                .when(authService).register(any(RegisterRequest.class));

        // When & Then
        mockMvc.perform(post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("An unexpected error occurred"));
    }

    // LOGIN TESTS

    @Test
    void shouldLoginUserAndSetCookieSuccessfully() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest("test@email.com", "password123");
        
        User mockUser = new User("1", "testuser", false);
        
        ResponseCookie mockCookie = ResponseCookie.from("ogs_session", "session-id-123").build();

        given(authService.login(any(LoginRequest.class))).willReturn(mockUser);
        given(sessionService.createSessionCookie(mockUser)).willReturn(mockCookie);

        // When & Then
        mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, mockCookie.toString()))
                .andExpect(content().string("Login successful"));
    }

    @Test
    void shouldReturnUnauthorizedOnInvalidCredentials() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest("test@email.com", "wrongpassword");
        given(authService.login(any(LoginRequest.class)))
                .willThrow(new InvalidCredentialsException("Bad credentials"));

        // When & Then
        mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Login failed: Invalid credentials"));
    }

    // GUEST TESTS

    @Test
    void shouldCreateGuestSessionSuccessfully() throws Exception {
        // Given
        User guestUser = new User("2", "Guest_123", true);
        
        ResponseCookie mockCookie = ResponseCookie.from("ogs_session", "guest-session-id").build();

        given(authService.createGuest()).willReturn(guestUser);
        given(sessionService.createSessionCookie(guestUser)).willReturn(mockCookie);

        // When & Then
        mockMvc.perform(post("/guest"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, mockCookie.toString()))
                .andExpect(content().string("Logged in as Guest"));
    }

    // LOGOUT TESTS

    @Test
    void shouldLogoutAndCleanCookie() throws Exception {
        // Given
        ResponseCookie cleanCookie = ResponseCookie.from("ogs_session", "").maxAge(0).build();
        given(sessionService.getCleanSessionCookie()).willReturn(cleanCookie);

        // When & Then
        mockMvc.perform(post("/logout"))
                .andExpect(status().isOk())
                .andExpect(cookie().value("ogs_session", ""))
                .andExpect(cookie().maxAge("ogs_session", 0))
                .andExpect(content().string("You've been signed out!"));

        verify(sessionService, times(1)).deleteSession(any());
    }
}