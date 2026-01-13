package com.online_games_service.authorization.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "accounts")
@Data
@NoArgsConstructor
public class Account {

    @Id
    private String id;

    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Invalid email format")
    @Indexed(unique = true)
    private String email;

    /**
     * Password hash for LOCAL authentication.
     * Null for OAuth-only accounts.
     */
    private String passwordHash;

    @NotBlank(message = "User ID cannot be blank")
    @Indexed(unique = true)
    private String userId;

    @NotBlank(message = "Username cannot be blank")
    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    private String username;

    /**
     * The authentication provider used to create this account.
     */
    @NotNull(message = "Auth provider cannot be null")
    private AuthProvider authProvider = AuthProvider.LOCAL;

    /**
     * Google's unique user identifier (sub claim).
     * Only set for accounts created via Google OAuth.
     */
    @Indexed(unique = true, sparse = true)
    private String googleId;

    /**
     * URL to the user's profile picture from OAuth provider.
     */
    private String pictureUrl;

    @CreatedDate
    private LocalDateTime createdAt;

    /**
     * Constructor for traditional email/password registration.
     */
    public Account(String email, String passwordHash, String userId, String username) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.userId = userId;
        this.username = username;
        this.authProvider = AuthProvider.LOCAL;
    }

    /**
     * Constructor for OAuth registration.
     */
    public Account(String email, String userId, String username, AuthProvider authProvider, String googleId, String pictureUrl) {
        this.email = email;
        this.userId = userId;
        this.username = username;
        this.authProvider = authProvider;
        this.googleId = googleId;
        this.pictureUrl = pictureUrl;
        this.passwordHash = null;
    }

    /**
     * Checks if this account was created via OAuth.
     */
    public boolean isOAuthAccount() {
        return authProvider != AuthProvider.LOCAL;
    }
}
