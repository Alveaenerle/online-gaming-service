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

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionService {
    private final SessionRedisRepository sessionRepository;

    @Value("${onlinegamesservice.app.jwtCookieName:ogs_session}")
    private String cookieName;

    @Value("${onlinegamesservice.app.sessionTimeout:86400}")
    private long sessionTimeout;

    public ResponseCookie createSessionCookie(User user) {
        String sessionId = UUID.randomUUID().toString();
        sessionRepository.saveSession(sessionId, user);
        return generateCookie(sessionId);
    }

    public User getUserFromCookie(HttpServletRequest request) {
        String sessionId = getSessionIdFromCookies(request);
        if (sessionId != null) {
            return sessionRepository.findUserBySessionId(sessionId).orElse(null);
        }
        return null;
    }

    public ResponseCookie getCleanSessionCookie() {
        return ResponseCookie.from(cookieName, "")
                .path("/")
                .maxAge(0)
                .build();
    }
    
    public void deleteSession(HttpServletRequest request) {
        String sessionId = getSessionIdFromCookies(request);
        if(sessionId != null) {
            sessionRepository.deleteSession(sessionId);
        }
    }

    private String getSessionIdFromCookies(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, cookieName);
        return (cookie != null) ? cookie.getValue() : null;
    }

    private ResponseCookie generateCookie(String value) {
        return ResponseCookie.from(cookieName, value)
                .path("/")        
                .maxAge(sessionTimeout)
                .httpOnly(true)   
                .secure(false)    
                .sameSite("Lax")  
                .build();
    }
}