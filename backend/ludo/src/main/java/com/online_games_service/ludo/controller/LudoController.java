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

    @GetMapping("/game-state")
    public ResponseEntity<LudoGameStateMessage> getGameState(@RequestAttribute("userId") String userId) {
        return ResponseEntity.ok(ludoService.getGameState(userId));
    }

    @PostMapping("/roll")
    public ResponseEntity<Map<String, String>> rollDice(@RequestAttribute("userId") String userId) {
        ludoService.rollDice(userId);
        return ResponseEntity.ok(Map.of("message", "Dice rolled"));
    }

    @PostMapping("/move")
    public ResponseEntity<Map<String, String>> movePawn(@RequestParam int pawnIndex,
                                                        @RequestAttribute("userId") String userId) {
        ludoService.movePawn(userId, pawnIndex);
        return ResponseEntity.ok(Map.of("message", "Move accepted"));
    }

    /**
     * Leave the current game. The player will be replaced by a bot.
     */
    @PostMapping("/leave-game")
    public ResponseEntity<Map<String, String>> leaveGame(
            @RequestAttribute(value = "userId", required = false) String userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        ludoService.handlePlayerLeave(userId);
        return ResponseEntity.ok(Map.of("message", "success"));
    }

    /**
     * Request the current game state to be sent via WebSocket.
     */
    @PostMapping("/request-state")
    public ResponseEntity<Map<String, String>> requestState(
            @RequestAttribute(value = "userId", required = false) String userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        ludoService.requestStateForUser(userId);
        return ResponseEntity.ok(Map.of("message", "success"));
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