package com.online_games_service.authorization.service;

import com.online_games_service.authorization.dto.GoogleUserInfo;
import com.online_games_service.authorization.dto.LoginRequest;
import com.online_games_service.authorization.dto.RegisterRequest;
import com.online_games_service.authorization.dto.UpdateUsernameRequest;
import com.online_games_service.authorization.dto.UpdatePasswordRequest;
import com.online_games_service.authorization.exception.EmailAlreadyExistsException;
import com.online_games_service.authorization.exception.InvalidCredentialsException;
import com.online_games_service.authorization.exception.OAuthAccountException;
import com.online_games_service.authorization.exception.UsernameAlreadyExistsException;
import com.online_games_service.authorization.model.Account;
import com.online_games_service.authorization.model.AuthProvider;
import com.online_games_service.authorization.model.User;
import com.online_games_service.authorization.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
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

        // Check if this is an OAuth account trying to login with password
        if (account.isOAuthAccount()) {
            throw new OAuthAccountException("This account uses " + account.getAuthProvider() + " sign-in. Please use that method to log in.");
        }

        if (!passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        return new User(
            account.getUserId(),
            account.getUsername(),
            false
        );
    }

    /**
     * Handles Google OAuth authentication.
     * If the user exists with the given Google ID, logs them in.
     * If the user exists with the same email (local account), links the Google account.
     * If the user doesn't exist, creates a new account.
     *
     * @param googleUserInfo Verified user information from Google
     * @return The authenticated User
     */
    @Transactional
    public User loginWithGoogle(GoogleUserInfo googleUserInfo) {
        log.info("Processing Google OAuth login for email: {}", googleUserInfo.getEmail());

        // First, try to find by Google ID
        Optional<Account> existingByGoogleId = accountRepository.findByGoogleId(googleUserInfo.getGoogleId());
        
        if (existingByGoogleId.isPresent()) {
            Account account = existingByGoogleId.get();
            log.info("Found existing account by Google ID for: {}", googleUserInfo.getEmail());
            
            // Update profile picture if changed
            if (googleUserInfo.getPictureUrl() != null && 
                !googleUserInfo.getPictureUrl().equals(account.getPictureUrl())) {
                account.setPictureUrl(googleUserInfo.getPictureUrl());
                accountRepository.save(account);
            }
            
            return new User(
                account.getUserId(),
                account.getUsername(),
                false
            );
        }

        // Check if email exists
        Optional<Account> existingByEmail = accountRepository.findByEmail(googleUserInfo.getEmail());
        
        if (existingByEmail.isPresent()) {
            Account account = existingByEmail.get();
            
            // If it's a local account, we need to link Google
            if (account.getAuthProvider() == AuthProvider.LOCAL) {
                log.info("Linking Google account to existing local account for: {}", googleUserInfo.getEmail());
                account.setGoogleId(googleUserInfo.getGoogleId());
                account.setAuthProvider(AuthProvider.GOOGLE);
                if (googleUserInfo.getPictureUrl() != null) {
                    account.setPictureUrl(googleUserInfo.getPictureUrl());
                }
                accountRepository.save(account);
            }
            
            return new User(
                account.getUserId(),
                account.getUsername(),
                false
            );
        }

        // Create new account
        log.info("Creating new account via Google OAuth for: {}", googleUserInfo.getEmail());
        String generatedUserId = UUID.randomUUID().toString();
        
        // Generate username from Google name or email
        String username = generateUsernameFromGoogleInfo(googleUserInfo);
        
        Account newAccount = new Account(
            googleUserInfo.getEmail(),
            generatedUserId,
            username,
            AuthProvider.GOOGLE,
            googleUserInfo.getGoogleId(),
            googleUserInfo.getPictureUrl()
        );

        accountRepository.save(newAccount);
        
        return new User(
            newAccount.getUserId(),
            newAccount.getUsername(),
            false
        );
    }

    /**
     * Generates a unique username from Google user info.
     */
    private String generateUsernameFromGoogleInfo(GoogleUserInfo googleUserInfo) {
        String baseName = googleUserInfo.getName();
        
        if (baseName == null || baseName.isBlank()) {
            // Fallback to email prefix
            baseName = googleUserInfo.getEmail().split("@")[0];
        }
        
        // Clean up the name: remove spaces, special chars, limit length
        baseName = baseName.replaceAll("[^a-zA-Z0-9]", "");
        
        if (baseName.length() > 15) {
            baseName = baseName.substring(0, 15);
        }
        
        if (baseName.length() < 3) {
            baseName = "User";
        }
        
        // Check if username exists, add random suffix if needed
        String username = baseName;
        int attempts = 0;
        while (accountRepository.existsByUsername(username) && attempts < 10) {
            username = baseName + "_" + (int)(Math.random() * 10000);
            attempts++;
        }
        
        if (attempts >= 10) {
            // Fallback to UUID-based username
            username = "User_" + UUID.randomUUID().toString().substring(0, 8);
        }
        
        return username;
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

    @Transactional
    public User updateUsername(String userId, UpdateUsernameRequest request) {
        Account account = accountRepository.findByUserId(userId)
            .orElseThrow(() -> new InvalidCredentialsException("Account not found"));

        // Check if the new username is already taken by another user
        String newUsername = request.getNewUsername();
        if (!account.getUsername().equals(newUsername) && accountRepository.existsByUsername(newUsername)) {
            throw new UsernameAlreadyExistsException("Username is already taken");
        }

        account.setUsername(newUsername);
        accountRepository.save(account);

        return new User(
            account.getUserId(),
            account.getUsername(),
            false
        );
    }

    @Transactional
    public void updatePassword(String userId, UpdatePasswordRequest request) {
        Account account = accountRepository.findByUserId(userId)
            .orElseThrow(() -> new InvalidCredentialsException("Account not found"));

        // OAuth accounts cannot update password
        if (account.isOAuthAccount() && account.getPasswordHash() == null) {
            throw new OAuthAccountException("OAuth accounts cannot update password. Please use your OAuth provider.");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), account.getPasswordHash())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        account.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        accountRepository.save(account);
    }

    public String getUserEmail(String userId) {
        Account account = accountRepository.findByUserId(userId)
            .orElseThrow(() -> new InvalidCredentialsException("Account not found"));
        return account.getEmail();
    }
}