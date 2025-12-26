package com.online_games_service.menu.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class RoomInfoResponse {
    private String name;
    private List<String> playersUsernames;
    private boolean isPrivate;
    private String accessCode;
    private String hostUsername;
}