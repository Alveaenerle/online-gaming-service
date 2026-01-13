package com.online_games_service.authorization.model;

/**
 * Enum representing the authentication provider used to create an account.
 */
public enum AuthProvider {
    /**
     * Traditional email/password authentication.
     */
    LOCAL,
    
    /**
     * Google OAuth authentication.
     */
    GOOGLE
}
