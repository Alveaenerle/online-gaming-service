package com.online_games_service.ludo.model;

import com.online_games_service.common.enums.RoomStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.ArrayList;

@Data
@NoArgsConstructor
public class LudoGame {
    
    private String id;
    private RoomStatus status;

    private List<LudoPlayer> players = new ArrayList<>();
    private String currentPlayerColor; 
    
    private int lastDiceRoll;
    private boolean diceRolled;      
    private boolean waitingForMove;  
    
    private String winnerId; 

    public LudoGame(String id, List<String> playerIds) {
        this.id = id;
        this.status = RoomStatus.PLAYING;
        
        String[] colors = {"RED", "GREEN", "BLUE", "YELLOW"};
        
        for (int i = 0; i < playerIds.size() && i < 4; i++) {
            this.players.add(new LudoPlayer(playerIds.get(i), colors[i]));
        }
        
        if (!players.isEmpty()) {
            this.currentPlayerColor = players.get(0).getColor();
        }
    }
}