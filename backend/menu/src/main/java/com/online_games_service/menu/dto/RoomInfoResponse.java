package com.online_games_service.menu.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.Map;
import com.online_games_service.common.enums.GameType;
import com.online_games_service.common.enums.RoomStatus;
import com.online_games_service.menu.model.PlayerState;

@Data
@AllArgsConstructor
public class RoomInfoResponse {
    private String id;
    private String name;
    private GameType gameType;
    private Map<String, PlayerState> players;
    private int maxPlayers;
    private boolean isPrivate;
    private String accessCode;
    private String hostUserId;
    private String hostUsername;
    private RoomStatus status;
}