package com.online_games_service.menu.model;

import com.online_games_service.common.enums.GameType;
import com.online_games_service.common.enums.RoomStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Document(collection = "game_rooms")
@Data
@NoArgsConstructor
public class GameRoom {

    @Id
    private String id;

    @NotBlank
    private String name;

    private GameType gameType;

    private String hostUsername;

    private Set<String> playersUsernames = new HashSet<>();

    private int maxPlayers;

    private boolean isPrivate;
    private String accessCode;

    private RoomStatus status;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public GameRoom(String name, GameType gameType, String hostUsername, int maxPlayers, boolean isPrivate) {
        this.name = name;
        this.gameType = gameType;
        this.hostUsername = hostUsername;
        this.maxPlayers = maxPlayers;
        this.isPrivate = isPrivate;
        this.status = RoomStatus.WAITING;
        this.playersUsernames.add(hostUsername);
    }
    
    public boolean canJoin() {
        return status == RoomStatus.WAITING && playersUsernames.size() < maxPlayers;
    }

    public Set<String> getPlayersUsernames() {
        return Collections.unmodifiableSet(playersUsernames);
    }

    public void setPlayersUsernames(Set<String> playersUsernames) {
        this.playersUsernames = playersUsernames != null ? playersUsernames : new HashSet<>();
    }

    public void addPlayer(String username) {
        if (!canJoin()) {
            throw new IllegalStateException("Cannot join room (Full or Game Started)");
        }
        this.playersUsernames.add(username);
        
        if (this.playersUsernames.size() >= maxPlayers) {
            this.status = RoomStatus.FULL;
        }
    }

    public void removePlayer(String username) {
        this.playersUsernames.remove(username);
        
        if (this.status == RoomStatus.FULL && this.playersUsernames.size() < maxPlayers) {
            this.status = RoomStatus.WAITING;
        }
    }
}