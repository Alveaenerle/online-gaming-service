package com.online_games_service.social.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(FriendRequestException.class)
    public ResponseEntity<Map<String, Object>> handleFriendRequestException(FriendRequestException ex) {
        logger.warn("Friend request error: {} - {}", ex.getErrorCode(), ex.getMessage());
        
        HttpStatus status = mapErrorCodeToStatus(ex.getErrorCode());
        
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("errorCode", ex.getErrorCode().name());
        body.put("message", ex.getMessage());
        
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        logger.warn("Validation error: {}", ex.getMessage());
        
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());
        body.put("errorCode", "VALIDATION_ERROR");
        
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            fieldErrors.put(error.getField(), error.getDefaultMessage())
        );
        body.put("fieldErrors", fieldErrors);
        
        return ResponseEntity.badRequest().body(body);
    }

    private HttpStatus mapErrorCodeToStatus(FriendRequestException.ErrorCode errorCode) {
        return switch (errorCode) {
            case SELF_REFERENTIAL_REQUEST -> HttpStatus.BAD_REQUEST;
            case USER_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case REQUEST_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case REQUEST_NOT_OWNED -> HttpStatus.FORBIDDEN;
            case ALREADY_FRIENDS -> HttpStatus.CONFLICT;
            case REQUEST_ALREADY_PENDING -> HttpStatus.CONFLICT;
            case REQUEST_ALREADY_ACCEPTED -> HttpStatus.CONFLICT;
            case DATABASE_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
