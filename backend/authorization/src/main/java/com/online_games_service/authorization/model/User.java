package com.online_games_service.authorization.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Represents an authenticated user or a guest in the system.
 * This model guarantees that ID and Username are never null or empty.
 */
@Data
@NoArgsConstructor 
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String username;
    private boolean isGuest;

    /**
     * Creates a new User with strict validation.
     *
     * @param id       Unique identifier, must not be null or blank.
     * @param username Display name, must not be null or blank.
     * @param isGuest  Flag indicating if the user is a temporary guest.
     * @throws IllegalArgumentException if id or username are invalid.
     */
    public User(String id, String username, boolean isGuest) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        
        this.id = id;
        this.username = username;
        this.isGuest = isGuest;
    }
}