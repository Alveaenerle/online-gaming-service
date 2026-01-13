package com.online_games_service.menu.messaging;

import com.online_games_service.common.messaging.PlayerLeaveMessage;
import com.online_games_service.menu.service.GameRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listens for player leave messages from game services (Makao, Ludo, etc.)
 * and updates the GameRoom accordingly (removes player, reassigns host if needed).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PlayerLeaveListener {

    private final GameRoomService gameRoomService;

    @RabbitListener(queues = "${menu.amqp.queue.leave:player.leave.queue}")
    public void handle(PlayerLeaveMessage message) {
        if (message == null || message.roomId() == null || message.playerId() == null) {
            log.warn("Received invalid player.leave message: {}", message);
            return;
        }

        log.info("Processing player leave: player {} from room {} (reason: {})", 
                 message.playerId(), message.roomId(), message.reason());

        try {
            gameRoomService.removePlayerFromRoom(message.roomId(), message.playerId());
        } catch (Exception ex) {
            log.error("Failed to process player.leave for player {} in room {}", 
                     message.playerId(), message.roomId(), ex);
            // Don't rethrow - we don't want to retry this message
        }
    }
}
