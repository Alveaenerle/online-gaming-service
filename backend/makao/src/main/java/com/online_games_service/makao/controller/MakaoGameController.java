package com.online_games_service.makao.controller;

import com.online_games_service.makao.dto.PlayCardRequest;
import com.online_games_service.makao.dto.DrawCardResponse;
import com.online_games_service.makao.dto.EndGameRequest;
import com.online_games_service.makao.service.MakaoGameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("")
@RequiredArgsConstructor
@Slf4j
public class MakaoGameController {

    private final MakaoGameService makaoGameService;

    @PostMapping("/play-card")
    public ResponseEntity<Map<String, String>> playCard(
            @RequestBody @Valid PlayCardRequest request,
            @RequestAttribute(value = "userId", required = false) String userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        makaoGameService.playCard(request, userId);
        return ResponseEntity.ok(successBody());
    }

    @PostMapping("/draw-card")
    public ResponseEntity<DrawCardResponse> drawCard(
            @RequestAttribute(value = "userId", required = false) String userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        DrawCardResponse response = makaoGameService.drawCard(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/play-drawn-card")
    public ResponseEntity<Map<String, String>> playDrawnCard(
            @RequestBody(required = false) PlayCardRequest request,
            @RequestAttribute(value = "userId", required = false) String userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        makaoGameService.playDrawnCard(request, userId);
        return ResponseEntity.ok(successBody());
    }

    @PostMapping("/skip-drawn-card")
    public ResponseEntity<Map<String, String>> skipDrawnCard(
            @RequestAttribute(value = "userId", required = false) String userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        makaoGameService.skipDrawnCard(userId);
        return ResponseEntity.ok(successBody());
    }

    @PostMapping("/accept-effect")
    public ResponseEntity<Map<String, String>> acceptEffect(
            @RequestAttribute(value = "userId", required = false) String userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        makaoGameService.acceptEffect(userId);
        return ResponseEntity.ok(successBody());
    }

    // for debugging purposes only
    // @PostMapping("/end-game")
    // public ResponseEntity<Map<String, String>> endGame(@RequestBody @Valid EndGameRequest request) {
    //     makaoGameService.forceEndGame(request);
    //     return ResponseEntity.ok(successBody());
    // }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        Map<String, String> body = new HashMap<>();
        body.put("error", "Game Error");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, String> body = new HashMap<>();
        body.put("error", "Validation Error");
        body.put("message", ex.getMessage());
        return ResponseEntity.badRequest().body(body);
    }

    private Map<String, String> successBody() {
        Map<String, String> body = new HashMap<>();
        body.put("message", "success");
        return body;
    }
}
