package com.online_games_service.menu.controller;

import com.online_games_service.menu.dto.CreateRoomRequest;
import com.online_games_service.menu.dto.JoinGameRequest;
import com.online_games_service.menu.dto.KickPlayerRequest;
import com.online_games_service.menu.dto.RoomInfoResponse;
import com.online_games_service.menu.model.GameRoom;
import com.online_games_service.menu.service.GameRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("")
@RequiredArgsConstructor
@Slf4j
public class GameRoomController {

    private final GameRoomService gameRoomService;

    @GetMapping("/room-info")
    public ResponseEntity<RoomInfoResponse> getRoomInfo(
            @RequestAttribute(value = "username", required = false) String username) {
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(gameRoomService.getPlayerRoomInfo(username));
    }

    @PostMapping("/create")
    public ResponseEntity<GameRoom> createRoom(
            @RequestBody @Valid CreateRoomRequest request,
            @RequestAttribute(value = "username", required = false) String username) {
        if (username == null) {
            log.warn("Unauthorized attempt to create game (No session found)");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(gameRoomService.createRoom(request, username));
    }

    @PostMapping("/join")
    public ResponseEntity<GameRoom> joinRoom(
            @RequestBody JoinGameRequest request,
            @RequestAttribute(value = "username", required = false) String username) {
        if (username == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(gameRoomService.joinRoom(request, username));
    }

    @PostMapping("/start")
    public ResponseEntity<GameRoom> startGame(
            @RequestAttribute(value = "username", required = false) String username) {
        if (username == null) {
            log.warn("Unauthorized attempt to start game (No session found)");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("Request to start game by user: {}", username);

        GameRoom startedRoom = gameRoomService.startGame(username);
        return ResponseEntity.ok(startedRoom);
    }

    @PostMapping("/leave")
    public ResponseEntity<Void> leaveRoom(@RequestAttribute(value = "username", required = false) String username) {
        if (username == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        gameRoomService.leaveRoom(username);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/kick-player")
    public ResponseEntity<Map<String, String>> kickPlayer(
            @RequestBody @Valid KickPlayerRequest request,
            @RequestAttribute(value = "username", required = false) String hostUsername) {
        if (hostUsername == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        String message = gameRoomService.kickPlayer(hostUsername, request.getUsername());

        Map<String, String> response = new HashMap<>();
        response.put("message", message);

        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        Map<String, String> response = new HashMap<>();
        response.put("error", "Game Error");
        response.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, String> response = new HashMap<>();
        response.put("error", "Validation Error");
        response.put("message", ex.getMessage());
        return ResponseEntity.badRequest().body(response);
    }
}