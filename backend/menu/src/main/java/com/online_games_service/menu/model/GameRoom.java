package com.online_games_service.menu.model;

import com.online_games_service.common.enums.GameType;
import com.online_games_service.common.enums.RoomStatus;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Document(collection = "game_rooms")
@Data
@NoArgsConstructor
public class GameRoom {

    @Id
    private String id;

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
}