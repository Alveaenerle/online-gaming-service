package com.online_games_service.common.messaging;

import com.online_games_service.common.enums.GameType;

import java.util.Map;

public record GameStartMessage(
        String roomId,
        String roomName,
        GameType gameType,
        Map<String, String> players,
        Map<String, String> playerAvatars,
        int maxPlayers,
        String hostUserId,
        String hostUsername
) {
}
