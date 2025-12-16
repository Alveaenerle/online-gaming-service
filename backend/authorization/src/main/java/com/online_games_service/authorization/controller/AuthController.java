package com.online_games_service.authorization.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

import com.online_games_service.authorization.dto.LoginRequest;
import com.online_games_service.authorization.dto.RegisterRequest;
import com.online_games_service.authorization.service.AuthService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; 

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequiredArgsConstructor
@Slf4j 
@CrossOrigin(origins = "${app.cors.allowed-origins}")
public class AuthController {
    private final AuthService authService;

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
            String token = authService.login(request);
            log.info("Login successful for email: {}", request.getEmail());
            return ResponseEntity.ok(Map.of("token", token));
        } catch(RuntimeException e) {
            log.warn("Login failed for email {}: {}", request.getEmail(), e.getMessage()); 
            return ResponseEntity.status(401).body("Login failed: " + e.getMessage());
        }
    }

    @PostMapping("/guest")
    public ResponseEntity<?> playAsGuest() {
        log.info("Creating guest session");
        String token = authService.createGuest();
        return ResponseEntity.ok(Map.of("token", token, "message", "Logged in as Guest"));
    }
}