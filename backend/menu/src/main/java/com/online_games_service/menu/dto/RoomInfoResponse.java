package com.online_games_service.menu.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;
import com.online_games_service.common.enums.GameType;

import com.online_games_service.common.enums.RoomStatus;

@Data
@AllArgsConstructor
public class RoomInfoResponse {
    private String name;
    private GameType gameType;
    private List<String> playersUsernames;
    private int maxPlayers;
    private boolean isPrivate;
    private String accessCode;
    private String hostUsername;
    private RoomStatus status;
}