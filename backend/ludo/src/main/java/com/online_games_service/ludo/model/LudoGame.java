package com.online_games_service.ludo.model;

import com.online_games_service.common.enums.RoomStatus;
import com.online_games_service.ludo.enums.PlayerColor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class LudoGame {
    
    private String id;
    private RoomStatus status;
    private List<LudoPlayer> players = new ArrayList<>();
    private PlayerColor currentPlayerColor; 
    
    private int lastDiceRoll;
    private boolean diceRolled;      
    private boolean waitingForMove;  

    private int rollsLeft; 

    private String winnerId; 

    public LudoGame(String id, List<String> playerIds) {
        this.id = id;
        this.status = RoomStatus.PLAYING;
        this.rollsLeft = 1;
        
        PlayerColor[] availableColors = PlayerColor.values();
        for (int i = 0; i < playerIds.size() && i < availableColors.length; i++) {
            this.players.add(new LudoPlayer(playerIds.get(i), availableColors[i]));
        }
        
        if (!players.isEmpty()) {
            this.currentPlayerColor = players.get(0).getColor();
        }
    }
}