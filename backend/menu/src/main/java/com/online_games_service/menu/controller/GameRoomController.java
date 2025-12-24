package com.online_games_service.menu.controller;

import com.online_games_service.menu.dto.CreateRoomRequest;
import com.online_games_service.menu.model.GameRoom;
import com.online_games_service.menu.service.GameRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("") 
@RequiredArgsConstructor
@Slf4j
public class GameRoomController {

    private final GameRoomService gameRoomService;

    @PostMapping("/create")
    public ResponseEntity<GameRoom> createRoom(
            @RequestBody @Valid CreateRoomRequest request,
            @RequestAttribute(value = "username", required = false) String username 
    ) {
        log.info("Request to create room: '{}'. Detected user: {}", 
                 request.getName(), 
                 (username != null ? username : "UNKNOWN"));

        if (username == null) {
            log.warn("Unauthorized attempt to create room (No session found)");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        GameRoom createdRoom = gameRoomService.createRoom(request, username);
        
        log.info("Room created successfully with ID: {}", createdRoom.getId());
        return ResponseEntity.ok(createdRoom);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(IllegalArgumentException ex) {
        Map<String, String> response = new HashMap<>();
        response.put("error", "Validation Error");
        response.put("message", ex.getMessage()); // Tu będzie Twój tekst o limitach
        
        return ResponseEntity.badRequest().body(response);
    }
}