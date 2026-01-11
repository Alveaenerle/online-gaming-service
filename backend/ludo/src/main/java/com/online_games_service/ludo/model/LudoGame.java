package com.online_games_service.ludo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.online_games_service.common.enums.RoomStatus;
import com.online_games_service.ludo.enums.PlayerColor;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.io.Serializable;
import java.util.*;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LudoGame implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String roomId;       
    private String gameId;   
    
    private RoomStatus status;
    private String hostUserId;
    private int maxPlayers;

    private List<LudoPlayer> players = new ArrayList<>();
    
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Map<String, String> playersUsernames = new HashMap<>(); 
    
    private PlayerColor currentPlayerColor;
    private String activePlayerId;
    
    private int lastDiceRoll;
    private boolean diceRolled;
    private boolean waitingForMove;
    private int rollsLeft; 

    private String winnerId;
    private Map<String, Integer> placement = new HashMap<>();
    
    private int botCounter = 0;

    public LudoGame(String roomId, List<String> playerIds, String hostUserId, Map<String, String> usernames) {
        this.roomId = roomId; 
        this.gameId = "LUDO-" + UUID.randomUUID().toString(); 
        
        this.status = RoomStatus.PLAYING;
        this.hostUserId = hostUserId;
        this.playersUsernames = usernames != null ? new HashMap<>(usernames) : new HashMap<>();
        this.maxPlayers = playerIds.size();
        this.rollsLeft = 1;

        PlayerColor[] availableColors = PlayerColor.values();
        for (int i = 0; i < playerIds.size() && i < availableColors.length; i++) {
            this.players.add(new LudoPlayer(playerIds.get(i), availableColors[i]));
        }

        if (!players.isEmpty()) {
            this.currentPlayerColor = players.get(0).getColor();
            this.activePlayerId = players.get(0).getUserId();
        }
    }
    
    public Map<String, String> getPlayersUsernames() {
        return new HashMap<>(this.playersUsernames);
    }

    public void setPlayersUsernames(Map<String, String> playersUsernames) {
        this.playersUsernames = playersUsernames != null ? new HashMap<>(playersUsernames) : new HashMap<>();
    }
    
    public LudoPlayer getPlayerById(String userId) {
        return players.stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElse(null); 
    }
    
    public LudoPlayer getPlayerByColor(PlayerColor color) {
        return players.stream()
                .filter(p -> p.getColor() == color)
                .findFirst()
                .orElse(null);
    }
}