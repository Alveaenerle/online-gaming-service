package com.online_games_service.ludo.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.ArrayList;

@Data
@NoArgsConstructor
public class LudoPlayer {
    private String userId;      
    private String color;       
    private List<LudoPawn> pawns; 

    public LudoPlayer(String userId, String color) {
        this.userId = userId;
        this.color = color;
        this.pawns = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            this.pawns.add(new LudoPawn(i, -1, true, false));
        }
    }
}