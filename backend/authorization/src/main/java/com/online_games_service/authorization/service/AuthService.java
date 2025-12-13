package com.online_games_service.authorization.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.online_games_service.authorization.security.jwt.JwtUtils;
import com.online_games_service.authorization.dto.LoginRequest;
import com.online_games_service.authorization.dto.RegisterRequest;
import com.online_games_service.authorization.model.Account;
import com.online_games_service.authorization.model.User;
import com.online_games_service.authorization.repository.AccountRepository;
import com.online_games_service.authorization.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Transactional
    public void register(RegisterRequest request) {
        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Error: Email is already in use!");
        }

        User newUser = new User(request.getUsername(), false);
        User savedUser = userRepository.save(newUser);

        Account newAccount = new Account(
            request.getEmail(), 
            passwordEncoder.encode(request.getPassword()), 
            savedUser.getId(),
            request.getUsername() 
        );
        accountRepository.save(newAccount);
    }

    public String login(LoginRequest request) {
        Account account = accountRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new RuntimeException("Account not found"));
        
        if(!passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
            throw new RuntimeException("Invalid password");
        }

        User user = userRepository.findById(account.getUserId())
            .orElseThrow(() -> new RuntimeException("User data integrity error"));
        
        return jwtUtils.generateJwtToken(user);
    }

    public String createGuest() {
        String guestName = "Guest_" + System.currentTimeMillis();
        User guestUser = new User(guestName, true);
        userRepository.save(guestUser);
        return jwtUtils.generateJwtToken(guestUser);
    }
}
