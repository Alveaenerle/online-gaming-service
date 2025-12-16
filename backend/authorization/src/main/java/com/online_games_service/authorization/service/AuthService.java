package com.online_games_service.authorization.service;

import com.online_games_service.authorization.dto.LoginRequest;
import com.online_games_service.authorization.dto.RegisterRequest;
import com.online_games_service.authorization.exception.EmailAlreadyExistsException; 
import com.online_games_service.authorization.exception.InvalidCredentialsException; 
import com.online_games_service.authorization.model.Account;
import com.online_games_service.authorization.model.User;
import com.online_games_service.authorization.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void register(RegisterRequest request) {
        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Error: Email is already in use!");
        }
        
        String generatedUserId = UUID.randomUUID().toString();

        Account newAccount = new Account(
            request.getEmail(), 
            passwordEncoder.encode(request.getPassword()), 
            generatedUserId, 
            request.getUsername() 
        );
        
        accountRepository.save(newAccount);
    }

    public User login(LoginRequest request) {
        Account account = accountRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));
        
        if (!passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        return new User(
            account.getUserId(), 
            account.getUsername(),
            false 
        );
    }

    public User createGuest() {
        String guestId = UUID.randomUUID().toString();
        String guestName = "Guest_" + System.currentTimeMillis();
        
        return new User(
            guestId, 
            guestName, 
            true 
        );
    }
}