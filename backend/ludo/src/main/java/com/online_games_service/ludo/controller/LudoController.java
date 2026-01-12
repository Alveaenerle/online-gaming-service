package com.online_games_service.ludo.controller;

import com.online_games_service.ludo.dto.LudoGameStateMessage;
import com.online_games_service.ludo.exception.GameLogicException;
import com.online_games_service.ludo.exception.InvalidMoveException;
import com.online_games_service.ludo.service.LudoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "${ludo.http.cors.allowed-origins}", allowCredentials = "true")
public class LudoController {

    private final LudoService ludoService;

    @GetMapping("/{gameId}")
    public ResponseEntity<LudoGameStateMessage> getGame(@PathVariable String gameId) {
        return ResponseEntity.ok(ludoService.getGameState(gameId));
    }

    @PostMapping("/{gameId}/roll")
    public ResponseEntity<Map<String, String>> rollDice(@PathVariable String gameId,
                                                        @RequestAttribute("userId") String userId) {

        ludoService.rollDice(gameId, userId);
        return ResponseEntity.ok(Map.of("message", "Dice rolled"));
    }

    @PostMapping("/{gameId}/move")
    public ResponseEntity<Map<String, String>> movePawn(@PathVariable String gameId,
                                                        @RequestParam int pawnIndex,
                                                        @RequestAttribute("userId") String userId) {

        ludoService.movePawn(gameId, userId, pawnIndex);
        return ResponseEntity.ok(Map.of("message", "Move accepted"));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "Game Error", "message", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", "Validation Error", "message", ex.getMessage()));
    }

    @ExceptionHandler(GameLogicException.class)
    public ResponseEntity<Map<String, String>> handleGameLogicException(GameLogicException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "Game Logic Error", "message", ex.getMessage()));
    }

    @ExceptionHandler(InvalidMoveException.class)
    public ResponseEntity<Map<String, String>> handleInvalidMoveException(InvalidMoveException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid Move", "message", ex.getMessage()));
    }
}