package com.online_games_service.authorization.controller;

import com.online_games_service.authorization.dto.LoginRequest;
import com.online_games_service.authorization.dto.RegisterRequest;
import com.online_games_service.authorization.model.User;
import com.online_games_service.authorization.service.AuthService;
import com.online_games_service.authorization.service.SessionService;
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
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest request) {
        log.info("Attempting to register user: {}", request.getUsername());
        try {
            authService.register(request);
            log.info("User registered successfully: {}", request.getUsername());
            return ResponseEntity.ok("User registered successfully!");
        } catch(RuntimeException e) {
            log.error("Registration failed for user {}: {}", request.getUsername(), e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());
        try {
            User user = authService.login(request);
            
            ResponseCookie sessionCookie = sessionService.createSessionCookie(user);
            
            log.info("Login successful for email: {}", request.getEmail());
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, sessionCookie.toString())
                    .body("Login successful");
        } catch(RuntimeException e) {
            log.warn("Login failed for email {}: {}", request.getEmail(), e.getMessage());
            return ResponseEntity.status(401).body("Login failed: " + e.getMessage());
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
    public ResponseEntity<?> logout(jakarta.servlet.http.HttpServletRequest request) {
        sessionService.deleteSession(request);
        ResponseCookie cookie = sessionService.getCleanSessionCookie();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body("You've been signed out!");
    }
}