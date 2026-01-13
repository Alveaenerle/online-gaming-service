package com.online_games_service.authorization.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.online_games_service.authorization.dto.GoogleOAuthRequest;
import com.online_games_service.authorization.dto.GoogleUserInfo;
import com.online_games_service.authorization.dto.LoginRequest;
import com.online_games_service.authorization.dto.RegisterRequest;
import com.online_games_service.authorization.dto.UpdateUsernameRequest;
import com.online_games_service.authorization.dto.UpdatePasswordRequest;
import com.online_games_service.authorization.exception.EmailAlreadyExistsException;
import com.online_games_service.authorization.exception.InvalidCredentialsException;
import com.online_games_service.authorization.exception.OAuthAccountException;
import com.online_games_service.authorization.exception.UsernameAlreadyExistsException;
import com.online_games_service.authorization.model.User;
import com.online_games_service.authorization.service.AuthService;
import com.online_games_service.authorization.service.GoogleTokenVerifierService;
import com.online_games_service.authorization.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.web.servlet.MockMvc;
import org.testng.annotations.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public class AuthControllerTest extends AbstractTestNGSpringContextTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private SessionService sessionService;

    @MockBean
    private GoogleTokenVerifierService googleTokenVerifierService;

    // REGSITER TESTS

    @Test
    public void shouldRegisterUserSuccessfully() throws Exception {
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
    public void shouldReturnBadRequestWhenEmailAlreadyExists() throws Exception {
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
    public void shouldReturnInternalServerErrorOnUnexpectedRegistrationError() throws Exception {
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

    @Test
    public void shouldReturnBadRequestWhenRegistrationRequestIsInvalid() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest("", "invalid-email", "");

        // When & Then
        mockMvc.perform(post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // LOGIN TESTS

    @Test
    public void shouldLoginUserAndSetCookieSuccessfully() throws Exception {
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
    public void shouldReturnUnauthorizedOnInvalidCredentials() throws Exception {
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

    @Test
    public void shouldReturnInternalServerErrorOnUnexpectedLoginError() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest("test@email.com", "password123");
        given(authService.login(any(LoginRequest.class)))
                .willThrow(new RuntimeException("Redis down"));

        // When & Then
        mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("An unexpected error occurred"));
    }

    @Test
    public void shouldReturnBadRequestWhenLoginRequestIsInvalid() throws Exception {
        // Given
        LoginRequest request = new LoginRequest("", "");

        // When & Then
        mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // GUEST TESTS

    @Test
    public void shouldCreateGuestSessionSuccessfully() throws Exception {
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
    public void shouldLogoutAndCleanCookie() throws Exception {
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

    // GET CURRENT USER TESTS

    @Test
    public void shouldReturnCurrentUserWhenAuthenticated() throws Exception {
        // Given
        User mockUser = new User("user-123", "testuser", false);
        given(sessionService.getUserFromCookie(any())).willReturn(mockUser);

        // When & Then
        mockMvc.perform(get("/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user-123"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.isGuest").value(false));
    }

    @Test
    public void shouldReturn401WhenNotAuthenticatedForGetMe() throws Exception {
        // Given
        given(sessionService.getUserFromCookie(any())).willReturn(null);

        // When & Then
        mockMvc.perform(get("/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Not authenticated"));
    }

    // UPDATE USERNAME TESTS

    @Test
    public void shouldUpdateUsernameSuccessfully() throws Exception {
        // Given
        User mockUser = new User("user-123", "oldusername", false);
        User updatedUser = new User("user-123", "newusername", false);
        UpdateUsernameRequest request = new UpdateUsernameRequest("newusername");
        ResponseCookie mockCookie = ResponseCookie.from("ogs_session", "session-id-123").build();

        given(sessionService.getUserFromCookie(any())).willReturn(mockUser);
        given(authService.updateUsername(eq("user-123"), any(UpdateUsernameRequest.class))).willReturn(updatedUser);
        given(sessionService.createSessionCookie(updatedUser)).willReturn(mockCookie);

        // When & Then
        mockMvc.perform(put("/update-username")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, mockCookie.toString()))
                .andExpect(jsonPath("$.id").value("user-123"))
                .andExpect(jsonPath("$.username").value("newusername"));
    }

    @Test
    public void shouldReturn401WhenNotAuthenticatedForUpdateUsername() throws Exception {
        // Given
        given(sessionService.getUserFromCookie(any())).willReturn(null);
        UpdateUsernameRequest request = new UpdateUsernameRequest("newusername");

        // When & Then
        mockMvc.perform(put("/update-username")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Not authenticated"));
    }

    @Test
    public void shouldReturn403WhenGuestTriesToUpdateUsername() throws Exception {
        // Given
        User guestUser = new User("guest-123", "Guest_123", true);
        given(sessionService.getUserFromCookie(any())).willReturn(guestUser);
        UpdateUsernameRequest request = new UpdateUsernameRequest("newusername");

        // When & Then
        mockMvc.perform(put("/update-username")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Guest accounts cannot update username"));
    }

    @Test
    public void shouldReturn409WhenUsernameAlreadyExists() throws Exception {
        // Given
        User mockUser = new User("user-123", "oldusername", false);
        UpdateUsernameRequest request = new UpdateUsernameRequest("takenusername");

        given(sessionService.getUserFromCookie(any())).willReturn(mockUser);
        given(authService.updateUsername(eq("user-123"), any(UpdateUsernameRequest.class)))
                .willThrow(new UsernameAlreadyExistsException("Username is already taken"));

        // When & Then
        mockMvc.perform(put("/update-username")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(content().string("{\"error\": \"Username is already taken\"}"));
    }

    @Test
    public void shouldReturn404WhenAccountNotFoundForUpdateUsername() throws Exception {
        // Given
        User mockUser = new User("user-123", "oldusername", false);
        UpdateUsernameRequest request = new UpdateUsernameRequest("newusername");

        given(sessionService.getUserFromCookie(any())).willReturn(mockUser);
        given(authService.updateUsername(eq("user-123"), any(UpdateUsernameRequest.class)))
                .willThrow(new InvalidCredentialsException("Account not found"));

        // When & Then
        mockMvc.perform(put("/update-username")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Account not found"));
    }

    @Test
    public void shouldReturnBadRequestForInvalidUsernameRequest() throws Exception {
        // Given
        User mockUser = new User("user-123", "oldusername", false);
        given(sessionService.getUserFromCookie(any())).willReturn(mockUser);

        // Empty username - validation should fail
        UpdateUsernameRequest request = new UpdateUsernameRequest("");

        // When & Then
        mockMvc.perform(put("/update-username")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnInternalServerErrorOnUnexpectedUpdateUsernameError() throws Exception {
        // Given
        User mockUser = new User("user-123", "oldusername", false);
        UpdateUsernameRequest request = new UpdateUsernameRequest("newusername");

        given(sessionService.getUserFromCookie(any())).willReturn(mockUser);
        given(authService.updateUsername(eq("user-123"), any(UpdateUsernameRequest.class)))
                .willThrow(new RuntimeException("Database error"));

        // When & Then
        mockMvc.perform(put("/update-username")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("An unexpected error occurred"));
    }

    // UPDATE PASSWORD TESTS

    @Test
    public void shouldUpdatePasswordSuccessfully() throws Exception {
        // Given
        User mockUser = new User("user-123", "testuser", false);
        UpdatePasswordRequest request = new UpdatePasswordRequest("oldPassword123", "newPassword456");
        ResponseCookie cleanCookie = ResponseCookie.from("ogs_session", "").maxAge(0).build();

        given(sessionService.getUserFromCookie(any())).willReturn(mockUser);
        doNothing().when(authService).updatePassword(eq("user-123"), any(UpdatePasswordRequest.class));
        given(sessionService.getCleanSessionCookie()).willReturn(cleanCookie);

        // When & Then
        mockMvc.perform(put("/update-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(cookie().value("ogs_session", ""))
                .andExpect(cookie().maxAge("ogs_session", 0))
                .andExpect(content().string("Password updated successfully"));

        verify(sessionService, times(1)).deleteSession(any());
    }

    @Test
    public void shouldReturn401WhenNotAuthenticatedForUpdatePassword() throws Exception {
        // Given
        given(sessionService.getUserFromCookie(any())).willReturn(null);
        UpdatePasswordRequest request = new UpdatePasswordRequest("oldPassword123", "newPassword456");

        // When & Then
        mockMvc.perform(put("/update-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Not authenticated"));
    }

    @Test
    public void shouldReturn403WhenGuestTriesToUpdatePassword() throws Exception {
        // Given
        User guestUser = new User("guest-123", "Guest_123", true);
        given(sessionService.getUserFromCookie(any())).willReturn(guestUser);
        UpdatePasswordRequest request = new UpdatePasswordRequest("oldPassword123", "newPassword456");

        // When & Then
        mockMvc.perform(put("/update-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Guest accounts cannot update password"));
    }

    @Test
    public void shouldReturn400WhenCurrentPasswordIsIncorrect() throws Exception {
        // Given
        User mockUser = new User("user-123", "testuser", false);
        UpdatePasswordRequest request = new UpdatePasswordRequest("wrongPassword", "newPassword456");

        given(sessionService.getUserFromCookie(any())).willReturn(mockUser);
        doThrow(new InvalidCredentialsException("Current password is incorrect"))
                .when(authService).updatePassword(eq("user-123"), any(UpdatePasswordRequest.class));

        // When & Then
        mockMvc.perform(put("/update-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Current password is incorrect"));
    }

    @Test
    public void shouldReturnBadRequestForInvalidPasswordRequest() throws Exception {
        // Given
        User mockUser = new User("user-123", "testuser", false);
        given(sessionService.getUserFromCookie(any())).willReturn(mockUser);

        // Empty passwords - validation should fail
        UpdatePasswordRequest request = new UpdatePasswordRequest("", "");

        // When & Then
        mockMvc.perform(put("/update-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnInternalServerErrorOnUnexpectedUpdatePasswordError() throws Exception {
        // Given
        User mockUser = new User("user-123", "testuser", false);
        UpdatePasswordRequest request = new UpdatePasswordRequest("oldPassword123", "newPassword456");

        given(sessionService.getUserFromCookie(any())).willReturn(mockUser);
        doThrow(new RuntimeException("Database error"))
                .when(authService).updatePassword(eq("user-123"), any(UpdatePasswordRequest.class));

        // When & Then
        mockMvc.perform(put("/update-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("An unexpected error occurred"));
    }

    // GET EMAIL TESTS

    @Test
    public void shouldReturnEmailSuccessfully() throws Exception {
        // Given
        User mockUser = new User("user-123", "testuser", false);
        given(sessionService.getUserFromCookie(any())).willReturn(mockUser);
        given(authService.getUserEmail("user-123")).willReturn("test@email.com");

        // When & Then
        mockMvc.perform(get("/email"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@email.com"));
    }

    @Test
    public void shouldReturn401WhenNotAuthenticatedForGetEmail() throws Exception {
        // Given
        given(sessionService.getUserFromCookie(any())).willReturn(null);

        // When & Then
        mockMvc.perform(get("/email"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Not authenticated"));
    }

    @Test
    public void shouldReturn403WhenGuestTriesToGetEmail() throws Exception {
        // Given
        User guestUser = new User("guest-123", "Guest_123", true);
        given(sessionService.getUserFromCookie(any())).willReturn(guestUser);

        // When & Then
        mockMvc.perform(get("/email"))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Guest accounts do not have email"));
    }

    @Test
    public void shouldReturn404WhenAccountNotFoundForGetEmail() throws Exception {
        // Given
        User mockUser = new User("user-123", "testuser", false);
        given(sessionService.getUserFromCookie(any())).willReturn(mockUser);
        given(authService.getUserEmail("user-123"))
                .willThrow(new InvalidCredentialsException("Account not found"));

        // When & Then
        mockMvc.perform(get("/email"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Account not found"));
    }

    @Test
    public void shouldReturnInternalServerErrorOnUnexpectedGetEmailError() throws Exception {
        // Given
        User mockUser = new User("user-123", "testuser", false);
        given(sessionService.getUserFromCookie(any())).willReturn(mockUser);
        given(authService.getUserEmail("user-123"))
                .willThrow(new RuntimeException("Database error"));

        // When & Then
        mockMvc.perform(get("/email"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("An unexpected error occurred"));
    }

    // GOOGLE OAUTH TESTS

    @Test
    public void shouldLoginWithGoogleSuccessfully() throws Exception {
        // Given
        GoogleOAuthRequest request = new GoogleOAuthRequest("valid-google-id-token");
        GoogleUserInfo googleUserInfo = GoogleUserInfo.builder()
                .googleId("google-123")
                .email("test@gmail.com")
                .emailVerified(true)
                .name("Test User")
                .pictureUrl("https://example.com/photo.jpg")
                .build();
        User mockUser = new User("user-id-123", "testuser", false);
        ResponseCookie mockCookie = ResponseCookie.from("ogs_session", "session-id").build();

        given(googleTokenVerifierService.isConfigured()).willReturn(true);
        given(googleTokenVerifierService.verifyToken("valid-google-id-token")).willReturn(googleUserInfo);
        given(authService.loginWithGoogle(any(GoogleUserInfo.class))).willReturn(mockUser);
        given(sessionService.createSessionCookie(mockUser)).willReturn(mockCookie);

        // When & Then
        mockMvc.perform(post("/oauth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, mockCookie.toString()))
                .andExpect(content().string("Login successful"));
    }

    @Test
    public void shouldReturn503WhenGoogleOAuthNotConfigured() throws Exception {
        // Given
        GoogleOAuthRequest request = new GoogleOAuthRequest("any-token");
        given(googleTokenVerifierService.isConfigured()).willReturn(false);

        // When & Then
        mockMvc.perform(post("/oauth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string("Google Sign-In is not available"));
    }

    @Test
    public void shouldReturn401WhenGoogleTokenIsInvalid() throws Exception {
        // Given
        GoogleOAuthRequest request = new GoogleOAuthRequest("invalid-token");
        given(googleTokenVerifierService.isConfigured()).willReturn(true);
        given(googleTokenVerifierService.verifyToken("invalid-token"))
                .willThrow(new InvalidCredentialsException("Invalid Google ID token"));

        // When & Then
        mockMvc.perform(post("/oauth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Google Sign-In failed: Invalid Google ID token"));
    }

    @Test
    public void shouldReturnInternalServerErrorOnUnexpectedGoogleOAuthError() throws Exception {
        // Given
        GoogleOAuthRequest request = new GoogleOAuthRequest("token");
        given(googleTokenVerifierService.isConfigured()).willReturn(true);
        given(googleTokenVerifierService.verifyToken("token"))
                .willThrow(new RuntimeException("Unexpected error"));

        // When & Then
        mockMvc.perform(post("/oauth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("An unexpected error occurred"));
    }

    @Test
    public void shouldReturnBadRequestWhenGoogleOAuthRequestIsInvalid() throws Exception {
        // Given
        GoogleOAuthRequest request = new GoogleOAuthRequest("");

        // When & Then
        mockMvc.perform(post("/oauth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnGoogleOAuthConfiguredStatus() throws Exception {
        // Given
        given(googleTokenVerifierService.isConfigured()).willReturn(true);

        // When & Then
        mockMvc.perform(get("/oauth/google/configured"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configured").value(true));
    }

    @Test
    public void shouldReturnGoogleOAuthNotConfiguredStatus() throws Exception {
        // Given
        given(googleTokenVerifierService.isConfigured()).willReturn(false);

        // When & Then
        mockMvc.perform(get("/oauth/google/configured"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configured").value(false));
    }

    @Test
    public void shouldReturnBadRequestWhenOAuthAccountTriesToLoginWithPassword() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest("oauth@gmail.com", "password");
        given(authService.login(any(LoginRequest.class)))
                .willThrow(new OAuthAccountException("This account uses GOOGLE sign-in"));

        // When & Then
        mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("This account uses GOOGLE sign-in"));
    }

    @Test
    public void shouldReturnBadRequestWhenOAuthAccountTriesToUpdatePassword() throws Exception {
        // Given
        User mockUser = new User("user-123", "oauthuser", false);
        UpdatePasswordRequest updateRequest = new UpdatePasswordRequest("currentPassword", "newPassword123");

        given(sessionService.getUserFromCookie(any())).willReturn(mockUser);
        doThrow(new OAuthAccountException("OAuth accounts cannot update password"))
                .when(authService).updatePassword(eq("user-123"), any(UpdatePasswordRequest.class));

        // When & Then
        mockMvc.perform(put("/update-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("OAuth accounts cannot update password"));
    }
}