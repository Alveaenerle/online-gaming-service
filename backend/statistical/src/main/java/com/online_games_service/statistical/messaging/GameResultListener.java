package com.online_games_service.statistical.messaging;

import com.online_games_service.common.messaging.GameResultMessage;
import com.online_games_service.statistical.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listens for game result messages from RabbitMQ and updates player statistics.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GameResultListener {

    private final StatisticsService statisticsService;

    @RabbitListener(queues = "${statistical.amqp.queue.game-result:statistical.game-result.queue}")
    public void handleGameResult(GameResultMessage message) {
        if (message == null) {
            log.warn("Received null GameResultMessage; skipping");
            return;
        }

        String roomId = message.roomId();
        String gameType = message.gameType();

        if (roomId == null || roomId.isBlank()) {
            log.warn("Received GameResultMessage without roomId; skipping");
            return;
        }

        if (gameType == null || gameType.isBlank()) {
            log.warn("Received GameResultMessage without gameType; skipping");
            return;
        }

        if (message.participants() == null || message.participants().isEmpty()) {
            log.warn("Received GameResultMessage without participants for room {}; skipping", roomId);
            return;
        }

        log.info("Processing game result for room {} ({}): {} participants", 
                roomId, gameType, message.participants().size());

        try {
            statisticsService.recordGameResult(
                    gameType,
                    message.participants(),
                    message.winnerId()
            );
            log.info("Successfully recorded game result for room {}", roomId);
        } catch (Exception e) {
            log.error("Failed to record game result for room {}: {}", roomId, e.getMessage(), e);
        }
    }
}
