package com.online_games_service.menu.controller;

import com.online_games_service.menu.dto.CreateRoomRequest;
import com.online_games_service.menu.dto.JoinGameRequest;
import com.online_games_service.menu.dto.KickPlayerRequest;
import com.online_games_service.menu.dto.RoomInfoResponse;
import com.online_games_service.menu.dto.UpdateAvatarRequest;
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
            @RequestAttribute(value = "userId", required = false) String userId,
            @RequestAttribute(value = "username", required = false) String username) {
        if (userId == null || username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(gameRoomService.getPlayerRoomInfo(userId, username));
    }

    @PostMapping("/create")
    public ResponseEntity<RoomInfoResponse> createRoom(
            @RequestBody @Valid CreateRoomRequest request,
            @RequestAttribute(value = "userId", required = false) String userId,
            @RequestAttribute(value = "username", required = false) String username) {
        if (userId == null || username == null) {
            log.warn("Unauthorized attempt to create game (No session found)");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        GameRoom room = gameRoomService.createRoom(request, userId, username);
        return ResponseEntity.ok(gameRoomService.buildRoomInfoResponse(room));
    }

    @PostMapping("/join")
    public ResponseEntity<RoomInfoResponse> joinRoom(
            @RequestBody JoinGameRequest request,
            @RequestAttribute(value = "userId", required = false) String userId,
            @RequestAttribute(value = "username", required = false) String username) {
        if (userId == null || username == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        GameRoom room = gameRoomService.joinRoom(request, userId, username);
        return ResponseEntity.ok(gameRoomService.buildRoomInfoResponse(room));
    }

    @PostMapping("/start")
    public ResponseEntity<RoomInfoResponse> startGame(
            @RequestAttribute(value = "userId", required = false) String userId,
            @RequestAttribute(value = "username", required = false) String username) {
        if (userId == null || username == null) {
            log.warn("Unauthorized attempt to start game (No session found)");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("Request to start game by user: {}", username);

        GameRoom startedRoom = gameRoomService.startGame(userId, username);
        return ResponseEntity.ok(gameRoomService.buildRoomInfoResponse(startedRoom));
    }

    @PostMapping("/leave")
    public ResponseEntity<Map<String, String>> leaveRoom(
            @RequestAttribute(value = "userId", required = false) String userId,
            @RequestAttribute(value = "username", required = false) String username) {
        if (userId == null || username == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        String message = gameRoomService.leaveRoom(userId, username);

        Map<String, String> response = new HashMap<>();
        response.put("message", message);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/kick-player")
    public ResponseEntity<Map<String, String>> kickPlayer(
            @RequestBody @Valid KickPlayerRequest request,
            @RequestAttribute(value = "userId", required = false) String hostUserId,
            @RequestAttribute(value = "username", required = false) String hostUsername) {
        if (hostUserId == null || hostUsername == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        String message = gameRoomService.kickPlayer(
            hostUserId,
            hostUsername,
            request.getUserId());

        Map<String, String> response = new HashMap<>();
        response.put("message", message);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/ready")
    public ResponseEntity<RoomInfoResponse> toggleReady(
            @RequestAttribute(value = "userId", required = false) String userId,
            @RequestAttribute(value = "username", required = false) String username) {
        if (userId == null || username == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        return ResponseEntity.ok(gameRoomService.toggleReady(userId, username));
    }

    @PostMapping("/update-avatar")
    public ResponseEntity<RoomInfoResponse> updateAvatar(
            @RequestBody @Valid UpdateAvatarRequest request,
            @RequestAttribute(value = "userId", required = false) String userId,
            @RequestAttribute(value = "username", required = false) String username) {
        if (userId == null || username == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        return ResponseEntity.ok(gameRoomService.updateAvatar(userId, username, request.getAvatarId()));
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