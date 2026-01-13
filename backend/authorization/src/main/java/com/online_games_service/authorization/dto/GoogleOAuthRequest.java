package com.online_games_service.authorization.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for Google OAuth authentication.
 * Contains the ID token received from Google Sign-In on the client side.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GoogleOAuthRequest {
    
    @NotBlank(message = "Google ID token is required")
    private String idToken;
}
