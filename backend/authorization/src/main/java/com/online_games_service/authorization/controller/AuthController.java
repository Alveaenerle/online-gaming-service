package com.online_games_service.authorization.controller;

import com.online_games_service.authorization.dto.LoginRequest;
import com.online_games_service.authorization.dto.RegisterRequest;
import com.online_games_service.authorization.dto.UpdateUsernameRequest;
import com.online_games_service.authorization.dto.UpdatePasswordRequest;
import com.online_games_service.authorization.exception.EmailAlreadyExistsException;
import com.online_games_service.authorization.exception.InvalidCredentialsException;
import com.online_games_service.authorization.model.User;
import com.online_games_service.authorization.service.AuthService;
import com.online_games_service.authorization.service.SessionService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "${app.cors.allowed-origins}", allowCredentials = "true")
public class AuthController {

    private final AuthService authService;
    private final SessionService sessionService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest request) {
        log.info("Attempting to register user: {}", request.getUsername());
        try {
            authService.register(request);
            log.info("User registered successfully: {}", request.getUsername());
            return ResponseEntity.ok("User registered successfully!");
        } catch (EmailAlreadyExistsException e) {
            log.warn("Registration failed for user {}: {}", request.getUsername(), e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during registration", e);
            return ResponseEntity.internalServerError().body("An unexpected error occurred");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());
        try {
            User user = authService.login(request);

            ResponseCookie sessionCookie = sessionService.createSessionCookie(user);

            log.info("Login successful for email: {}", request.getEmail());
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, sessionCookie.toString())
                    .body("Login successful");
        } catch (InvalidCredentialsException e) {
            log.warn("Login failed for email {}: {}", request.getEmail(), e.getMessage());
            return ResponseEntity.status(401).body("Login failed: Invalid credentials");
        } catch (Exception e) {
            log.error("Unexpected error during login", e);
            return ResponseEntity.internalServerError().body("An unexpected error occurred");
        }
    }

    @PostMapping("/guest")
    public ResponseEntity<?> playAsGuest() {
        log.info("Creating guest session");
        User guest = authService.createGuest();

        ResponseCookie sessionCookie = sessionService.createSessionCookie(guest);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionCookie.toString())
                .body("Logged in as Guest");
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        sessionService.deleteSession(request);
        ResponseCookie cookie = sessionService.getCleanSessionCookie();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body("You've been signed out!");
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        User user = sessionService.getUserFromCookie(request);
        if (user == null) {
            log.debug("No active session found for /me request");
            return ResponseEntity.status(401).body("Not authenticated");
        }
        log.debug("User {} retrieved from session", user.getUsername());
        return ResponseEntity.ok(user);
    }

    @PutMapping("/update-username")
    public ResponseEntity<?> updateUsername(HttpServletRequest request, @Valid @RequestBody UpdateUsernameRequest updateRequest) {
        User user = sessionService.getUserFromCookie(request);
        if (user == null) {
            log.debug("No active session found for /update-username request");
            return ResponseEntity.status(401).body("Not authenticated");
        }

        if (user.isGuest()) {
            log.warn("Guest user attempted to update username");
            return ResponseEntity.status(403).body("Guest accounts cannot update username");
        }

        log.info("Attempting to update username for user: {}", user.getId());
        try {
            User updatedUser = authService.updateUsername(user.getId(), updateRequest);

            // Update session with new user data
            ResponseCookie sessionCookie = sessionService.createSessionCookie(updatedUser);

            log.info("Username updated successfully for user: {}", user.getId());
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, sessionCookie.toString())
                    .body(updatedUser);
        } catch (InvalidCredentialsException e) {
            log.warn("Username update failed for user {}: {}", user.getId(), e.getMessage());
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during username update", e);
            return ResponseEntity.internalServerError().body("An unexpected error occurred");
        }
    }

    @PutMapping("/update-password")
    public ResponseEntity<?> updatePassword(HttpServletRequest request, @Valid @RequestBody UpdatePasswordRequest updateRequest) {
        User user = sessionService.getUserFromCookie(request);
        if (user == null) {
            log.debug("No active session found for /update-password request");
            return ResponseEntity.status(401).body("Not authenticated");
        }

        if (user.isGuest()) {
            log.warn("Guest user attempted to update password");
            return ResponseEntity.status(403).body("Guest accounts cannot update password");
        }

        log.info("Attempting to update password for user: {}", user.getId());
        try {
            authService.updatePassword(user.getId(), updateRequest);
            log.info("Password updated successfully for user: {}", user.getId());
            return ResponseEntity.ok("Password updated successfully");
        } catch (InvalidCredentialsException e) {
            log.warn("Password update failed for user {}: {}", user.getId(), e.getMessage());
            return ResponseEntity.status(400).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during password update", e);
            return ResponseEntity.internalServerError().body("An unexpected error occurred");
        }
    }

    @GetMapping("/email")
    public ResponseEntity<?> getUserEmail(HttpServletRequest request) {
        User user = sessionService.getUserFromCookie(request);
        if (user == null) {
            log.debug("No active session found for /email request");
            return ResponseEntity.status(401).body("Not authenticated");
        }

        if (user.isGuest()) {
            log.debug("Guest user requested email");
            return ResponseEntity.status(403).body("Guest accounts do not have email");
        }

        try {
            String email = authService.getUserEmail(user.getId());
            return ResponseEntity.ok(email);
        } catch (InvalidCredentialsException e) {
            log.warn("Email retrieval failed for user {}: {}", user.getId(), e.getMessage());
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during email retrieval", e);
            return ResponseEntity.internalServerError().body("An unexpected error occurred");
        }
    }
}