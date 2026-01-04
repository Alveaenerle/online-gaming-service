package com.online_games_service.ludo.model;

import com.online_games_service.ludo.enums.PlayerColor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class LudoPlayer {
    
    private String userId;
    private PlayerColor color;
    private List<LudoPawn> pawns;

    public LudoPlayer(String userId, PlayerColor color) {
        this.userId = userId;
        this.color = color;
        this.pawns = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            this.pawns.add(new LudoPawn(i, -1, color, 0, true, false)); 
        }
    }

    public boolean allPawnsInBase() {
        return pawns.stream().allMatch(LudoPawn::isInBase);
    }
}