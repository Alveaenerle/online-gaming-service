package com.online_games_service.authorization.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing user information extracted from a verified Google ID token.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GoogleUserInfo {
    
    /**
     * The unique Google user ID (sub claim from the ID token).
     */
    private String googleId;
    
    /**
     * The user's email address.
     */
    private String email;
    
    /**
     * Whether the email has been verified by Google.
     */
    private boolean emailVerified;
    
    /**
     * The user's display name.
     */
    private String name;
    
    /**
     * URL to the user's profile picture.
     */
    private String pictureUrl;
}
