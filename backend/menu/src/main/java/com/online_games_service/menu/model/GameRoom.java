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
import java.util.LinkedHashMap;
import java.util.Map;

@Document(collection = "game_rooms")
@Data
@NoArgsConstructor
public class GameRoom {

    @Id
    private String id;

    @NotBlank
    private String name;

    private GameType gameType;

    private String hostUserId;
    private String hostUsername;

    private Map<String, String> players = new LinkedHashMap<>(); // userId -> username

    private int maxPlayers;

    private boolean isPrivate;
    private String accessCode;

    private RoomStatus status;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public GameRoom(String name, GameType gameType, String hostUserId, String hostUsername, int maxPlayers, boolean isPrivate) {
        this.name = name;
        this.gameType = gameType;
        this.hostUserId = hostUserId;
        this.hostUsername = hostUsername;
        this.maxPlayers = maxPlayers;
        this.isPrivate = isPrivate;
        this.status = RoomStatus.WAITING;
        this.players.put(hostUserId, hostUsername);

        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public boolean canJoin() {
        return status == RoomStatus.WAITING && players.size() < maxPlayers;
    }

    public void addPlayer(String userId, String username) {
        if (!canJoin()) {
            throw new IllegalStateException("Cannot join room (Full or Game Started)");
        }
        if (!this.players.containsKey(userId)) {
            this.players.put(userId, username);
            this.updatedAt = LocalDateTime.now();
        }

        if (this.players.size() >= maxPlayers) {
            this.status = RoomStatus.FULL;
        }
    }

    public void removePlayerById(String userId) {
        String removedUsername = this.players.remove(userId);

        if (removedUsername != null) {
            this.updatedAt = LocalDateTime.now();

            if (this.status == RoomStatus.FULL && this.players.size() < maxPlayers) {
                this.status = RoomStatus.WAITING;
            }

            if (!this.players.isEmpty() && userId.equals(this.hostUserId)) {
                Map.Entry<String, String> nextHost = this.players.entrySet().iterator().next();
                this.hostUserId = nextHost.getKey();
                this.hostUsername = nextHost.getValue();
            }
        }
    }
}