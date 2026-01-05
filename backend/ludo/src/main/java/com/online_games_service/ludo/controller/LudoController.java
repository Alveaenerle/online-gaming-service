package com.online_games_service.ludo.controller;

import com.online_games_service.ludo.model.LudoGame;
import com.online_games_service.ludo.service.LudoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ludo")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") 
public class LudoController {

    private final LudoService ludoService;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/create-custom")
    public ResponseEntity<LudoGame> createGameWithId(
            @RequestParam String gameId,
            @RequestParam List<String> playerIds) {
        
        LudoGame game = ludoService.createGameWithId(gameId, playerIds);
        return ResponseEntity.ok(game);
    }

    @GetMapping("/{gameId}")
    public ResponseEntity<LudoGame> getGame(@PathVariable String gameId) {
        return ResponseEntity.ok(ludoService.getGame(gameId));
    }

    @PostMapping("/{gameId}/roll")
    public ResponseEntity<LudoGame> rollDice(@PathVariable String gameId, 
                                             @RequestParam String playerId) {
        LudoGame game = ludoService.rollDice(gameId, playerId);
        notifyPlayers(gameId, game);
        return ResponseEntity.ok(game);
    }

    @PostMapping("/{gameId}/move")
    public ResponseEntity<LudoGame> movePawn(@PathVariable String gameId, 
                                             @RequestParam String playerId, 
                                             @RequestParam int pawnIndex) {
        LudoGame game = ludoService.movePawn(gameId, playerId, pawnIndex);
        notifyPlayers(gameId, game);
        return ResponseEntity.ok(game);
    }

    private void notifyPlayers(String gameId, LudoGame game) {
        messagingTemplate.convertAndSend("/topic/game/" + gameId, game);
    }
}