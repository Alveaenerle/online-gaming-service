package com.online_games_service.ludo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "ludo_game_results")
public class LudoGameResult {
    @Id
    private String gameId;
    private int maxPlayers;
    private Map<String, String> players; 
    private String winnerId;
    private Map<String, Integer> placement; 
}