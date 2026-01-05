package com.online_games_service.makao.model;

import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "makao_game_results")
public class MakaoGameResult {

    @Id
    private String gameId;
    private int maxPlayers;
    private Map<String, String> players;
    private Map<String, Integer> ranking;
    private Map<String, Integer> placement;
}
