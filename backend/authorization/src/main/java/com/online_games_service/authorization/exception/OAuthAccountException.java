package com.online_games_service.authorization.exception;

/**
 * Exception thrown when an OAuth-specific error occurs.
 * For example, when a user with an OAuth account tries to login with password,
 * or when an OAuth account tries to update password.
 */
public class OAuthAccountException extends RuntimeException {
    
    public OAuthAccountException(String message) {
        super(message);
    }
}
