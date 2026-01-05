package com.online_games_service.ludo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class LudoExceptionHandler {

    @ExceptionHandler({IllegalStateException.class, IllegalArgumentException.class})
    public ResponseEntity<Object> handleGameExceptions(RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST) 
                .body(Map.of(
                        "error", "Game Logic Error",
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(RuntimeException.class) 
    public ResponseEntity<Object> handleRuntime(RuntimeException ex) {
        if (ex.getMessage().contains("Game not found")) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND) 
                    .body(Map.of("error", "Not Found", "message", ex.getMessage()));
        }
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Server Error", "message", ex.getMessage()));
    }
}