package com.online_games_service.common.messaging;

import com.online_games_service.common.enums.RoomStatus;

public record GameFinishMessage(
        String roomId,
        RoomStatus status
) {
}
