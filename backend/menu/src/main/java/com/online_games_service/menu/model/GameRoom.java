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
import java.util.ArrayList;
import java.util.List;

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

    private List<String> playersUsernames = new ArrayList<>();

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

        if (this.playersUsernames == null) {
            this.playersUsernames = new ArrayList<>();
        }
        this.playersUsernames.add(hostUsername);

        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public boolean canJoin() {
        return status == RoomStatus.WAITING && playersUsernames.size() < maxPlayers;
    }

    public void addPlayer(String username) {
        if (!canJoin()) {
            throw new IllegalStateException("Cannot join room (Full or Game Started)");
        }
        if (!this.playersUsernames.contains(username)) {
            this.playersUsernames.add(username);
            this.updatedAt = LocalDateTime.now();
        }
        
        if (this.playersUsernames.size() >= maxPlayers) {
            this.status = RoomStatus.FULL;
        }
    }

    public void removePlayer(String username) {
        boolean removed = this.playersUsernames.remove(username);
        
        if (removed) {
            this.updatedAt = LocalDateTime.now();

            if (this.status == RoomStatus.FULL && this.playersUsernames.size() < maxPlayers) {
                this.status = RoomStatus.WAITING;
            }

            if (!this.playersUsernames.isEmpty() && username.equals(this.hostUsername)) {
                this.hostUsername = this.playersUsernames.get(0);
            }
        }
    }

    public void setPlayersUsernames(List<String> playersUsernames) {
        this.playersUsernames = playersUsernames != null ? playersUsernames : new ArrayList<>();
    }
}