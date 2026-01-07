package com.online_games_service.menu.messaging;

import com.online_games_service.common.messaging.GameFinishMessage;
import com.online_games_service.menu.service.GameRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GameFinishListener {

    private final GameRoomService gameRoomService;

    @RabbitListener(queues = "${menu.amqp.queue.finish.makao:makao.finish.queue}")
    public void handle(GameFinishMessage message) {
        if (message == null || message.roomId() == null || message.roomId().isBlank()) {
            log.warn("Received invalid makao.finish message");
            return;
        }

        try {
            gameRoomService.markFinished(message.roomId(), message.status());
        } catch (Exception ex) {
            log.error("Failed to process makao.finish for room {}", message.roomId(), ex);
            throw ex;
        }
    }
}
