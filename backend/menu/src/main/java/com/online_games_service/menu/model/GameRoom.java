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

    private String hostId;

    private Set<String> playerIds = new HashSet<>();

    private int maxPlayers;

    private boolean isPrivate;
    private String accessCode;

    private RoomStatus status;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public GameRoom(String name, GameType gameType, String hostId, int maxPlayers, boolean isPrivate) {
        this.name = name;
        this.gameType = gameType;
        this.hostId = hostId;
        this.maxPlayers = maxPlayers;
        this.isPrivate = isPrivate;
        this.status = RoomStatus.WAITING;
        this.playerIds.add(hostId);
    }
    
    public boolean canJoin() {
        return status == RoomStatus.WAITING && playerIds.size() < maxPlayers;
    }

    public Set<String> getPlayerIds() {
        return Collections.unmodifiableSet(playerIds);
    }

    public void setPlayerIds(Set<String> playerIds) {
        this.playerIds = playerIds != null ? playerIds : new HashSet<>();
    }

    public void addPlayer(String playerId) {
        if (!canJoin()) {
            throw new IllegalStateException("Cannot join room (Full or Game Started)");
        }
        this.playerIds.add(playerId);
        
        if (this.playerIds.size() >= maxPlayers) {
            this.status = RoomStatus.FULL;
        }
    }

    public void removePlayer(String playerId) {
        this.playerIds.remove(playerId);
        
        if (this.status == RoomStatus.FULL && this.playerIds.size() < maxPlayers) {
            this.status = RoomStatus.WAITING;
        }
    }
}