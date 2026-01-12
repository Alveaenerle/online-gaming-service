package com.online_games_service.social.messaging;

import com.online_games_service.common.messaging.GameStartMessage;
import com.online_games_service.social.service.GameInviteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listener for game start events from RabbitMQ.
 * When a game starts, all pending invites for that lobby are deleted.
 */
@Component
public class GameStartListener {

    private static final Logger logger = LoggerFactory.getLogger(GameStartListener.class);

    private final GameInviteService gameInviteService;

    public GameStartListener(GameInviteService gameInviteService) {
        this.gameInviteService = gameInviteService;
    }

    /**
     * Handles game start messages from makao.start and ludo.start routing keys.
     * Deletes all pending invites for the lobby that just started.
     */
    @RabbitListener(queues = "social.game-start.queue")
    public void handleGameStart(GameStartMessage message) {
        if (message == null) {
            logger.warn("Received null GameStartMessage; skipping");
            return;
        }

        String roomId = message.roomId();
        if (roomId == null || roomId.isBlank()) {
            logger.warn("Received GameStartMessage without roomId; skipping");
            return;
        }

        logger.info("Game started for lobby {}, deleting all pending invites", roomId);
        
        try {
            gameInviteService.deleteInvitesForLobby(roomId);
            logger.debug("Successfully deleted invites for lobby {}", roomId);
        } catch (Exception e) {
            logger.error("Failed to delete invites for lobby {}: {}", roomId, e.getMessage(), e);
        }
    }
}
