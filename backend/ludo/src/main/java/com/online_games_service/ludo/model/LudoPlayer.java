package com.online_games_service.ludo.model;

import com.online_games_service.ludo.enums.PlayerColor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class LudoPlayer implements Serializable {
    private String userId;
    private PlayerColor color;
    private List<LudoPawn> pawns;
    private boolean isBot; 

    public LudoPlayer(String userId, PlayerColor color) {
        this.userId = userId;
        this.color = color;
        this.isBot = userId.startsWith("bot-");
        this.pawns = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            this.pawns.add(new LudoPawn(i, -1, color, 0, true, false));
        }
    }
    
    public boolean hasAllPawnsInHome() {
        return pawns.stream().allMatch(LudoPawn::isInHome);
    }
}