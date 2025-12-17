package com.online_games_service.authorization.service;

import com.online_games_service.authorization.model.User;
import com.online_games_service.authorization.repository.redis.SessionRedisRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.web.util.WebUtils;

import java.time.Duration;
import java.util.UUID;

/**
 * Service responsible for managing user sessions using a stateful approach backed by Redis.
 * <p>
 * This service implements the "Opaque Token" pattern where a random Session ID (UUID)
 * is sent to the client as a Cookie, while the actual User data is stored securely in Redis.
 * </p>
 *
 * <h3>Security Strategy:</h3>
 * <ul>
 * <li><strong>HttpOnly:</strong> Enabled. JavaScript cannot access the cookie, mitigating XSS attacks.</li>
 * <li><strong>SameSite:</strong> Set to 'Lax'. Provides reasonable protection against CSRF while allowing top-level navigation.</li>
 * <li><strong>Secure:</strong> Currently disabled (false) for local development ease. <strong>Must be enabled in production (HTTPS).</strong></li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class SessionService {
    private final SessionRedisRepository sessionRepository;

    @Value("${onlinegamesservice.app.sessionCookieName:ogs_session}")
    private String cookieName;

    @Value("${onlinegamesservice.app.sessionTimeout:86400}")
    private long sessionTimeout;

    /**
     * Creates a new session for the authenticated user.
     * <p>
     * This method performs two main actions:
     * <ol>
     * <li>Generates a random UUID and saves the User object in Redis with the configured timeout.</li>
     * <li>Generates an HTTP Set-Cookie header containing the Session ID.</li>
     * </ol>
     * </p>
     *
     * @param user The authenticated user model to be stored in the session.
     * @return A {@link ResponseCookie} containing the session ID and security flags.
     */
    public ResponseCookie createSessionCookie(User user) {
        String sessionId = UUID.randomUUID().toString();
        sessionRepository.saveSession(sessionId, user);
        return generateCookie(sessionId);
    }

    /**
     * Retrieves the User associated with the session cookie from the current request.
     *
     * @param request The incoming HTTP request containing the cookies.
     * @return The {@link User} object if the session exists and is valid in Redis; {@code null} otherwise.
     */
    public User getUserFromCookie(HttpServletRequest request) {
        String sessionId = getSessionIdFromCookies(request);
        if (sessionId != null) {
            return sessionRepository.findUserBySessionId(sessionId).orElse(null);
        }
        return null;
    }

    /**
     * Generates a "clean" cookie to invalidate the session on the client side.
     * <p>
     * This returns a cookie with the same name but an empty value and a Max-Age of 0,
     * instructing the browser to delete it immediately.
     * </p>
     *
     * @return A {@link ResponseCookie} that clears the session cookie.
     */
    public ResponseCookie getCleanSessionCookie() {
        return ResponseCookie.from(cookieName, "")
                .path("/")
                .maxAge(0)
                .build();
    }

    /**
     * Invalidates the session on the server side (Redis).
     * <p>
     * This method extracts the Session ID from the request cookie and deletes
     * the corresponding key from Redis, effectively logging the user out
     * even if they still possess the cookie.
     * </p>
     *
     * @param request The incoming HTTP request.
     */
    public void deleteSession(HttpServletRequest request) {
        String sessionId = getSessionIdFromCookies(request);
        if (sessionId != null) {
            sessionRepository.deleteSession(sessionId);
        }
    }

    private String getSessionIdFromCookies(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, cookieName);
        return (cookie != null) ? cookie.getValue() : null;
    }

    private ResponseCookie generateCookie(String value) {
        // Generates a session cookie with security flags:
        // - HttpOnly: Prevents JavaScript access to mitigate XSS.
        // - SameSite=Lax: Helps protect against CSRF by restricting cross-site requests.
        // - Secure: Should be enabled in production to ensure cookies are sent only over HTTPS.
        return ResponseCookie.from(cookieName, value)
                .path("/")
                .maxAge(Duration.ofSeconds(sessionTimeout)) 
                .httpOnly(true)   // Mitigates XSS: Client-side JS cannot read this cookie
                .secure(false)    // TODO: Set to true in production (requires HTTPS)
                .sameSite("Lax")  // Mitigates CSRF: Cookie not sent on cross-site subrequests
                .build();
    }
}