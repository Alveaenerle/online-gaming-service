package com.online_games_service.authorization.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.online_games_service.authorization.dto.GoogleUserInfo;
import com.online_games_service.authorization.exception.InvalidCredentialsException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

/**
 * Service responsible for verifying Google ID tokens and extracting user information.
 * <p>
 * This service uses Google's official API client library to verify ID tokens
 * received from the client-side Google Sign-In flow.
 * </p>
 */
@Service
@Slf4j
public class GoogleTokenVerifierService {

    @Value("${oauth.google.client-id:}")
    private String googleClientId;

    private GoogleIdTokenVerifier verifier;

    @PostConstruct
    public void init() {
        if (googleClientId != null && !googleClientId.isBlank()) {
            this.verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();
            log.info("Google OAuth token verifier initialized with client ID");
        } else {
            log.warn("Google OAuth client ID not configured. Google Sign-In will be disabled.");
        }
    }

    /**
     * Allows setting a custom verifier (primarily for testing).
     */
    public void setVerifier(GoogleIdTokenVerifier verifier) {
        this.verifier = verifier;
    }

    /**
     * Verifies a Google ID token and extracts user information.
     *
     * @param idTokenString The ID token string from the client.
     * @return GoogleUserInfo containing the user's information.
     * @throws InvalidCredentialsException if the token is invalid or verification fails.
     */
    public GoogleUserInfo verifyToken(String idTokenString) {
        if (verifier == null) {
            log.error("Google OAuth is not configured. Cannot verify token.");
            throw new InvalidCredentialsException("Google Sign-In is not configured");
        }

        if (idTokenString == null || idTokenString.isBlank()) {
            throw new InvalidCredentialsException("ID token cannot be empty");
        }

        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            
            if (idToken == null) {
                log.warn("Invalid Google ID token received");
                throw new InvalidCredentialsException("Invalid Google ID token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            
            String googleId = payload.getSubject();
            String email = payload.getEmail();
            boolean emailVerified = payload.getEmailVerified();
            String name = (String) payload.get("name");
            String pictureUrl = (String) payload.get("picture");

            if (!emailVerified) {
                log.warn("Email not verified for Google account: {}", email);
                throw new InvalidCredentialsException("Email is not verified with Google");
            }

            log.debug("Successfully verified Google token for user: {}", email);
            
            return GoogleUserInfo.builder()
                    .googleId(googleId)
                    .email(email)
                    .emailVerified(emailVerified)
                    .name(name)
                    .pictureUrl(pictureUrl)
                    .build();

        } catch (GeneralSecurityException e) {
            log.error("Security exception while verifying Google token", e);
            throw new InvalidCredentialsException("Failed to verify Google token: security error");
        } catch (IOException e) {
            log.error("IO exception while verifying Google token", e);
            throw new InvalidCredentialsException("Failed to verify Google token: network error");
        } catch (IllegalArgumentException e) {
            log.error("Invalid token format", e);
            throw new InvalidCredentialsException("Invalid Google token format");
        }
    }

    /**
     * Checks if Google OAuth is properly configured.
     *
     * @return true if Google OAuth is configured and ready to use.
     */
    public boolean isConfigured() {
        return verifier != null;
    }
}
