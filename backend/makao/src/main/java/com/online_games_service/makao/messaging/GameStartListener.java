package com.online_games_service.makao.messaging;

import com.online_games_service.common.messaging.GameStartMessage;
import com.online_games_service.makao.model.MakaoGame;
import com.online_games_service.makao.repository.redis.MakaoGameRedisRepository;
import com.online_games_service.makao.service.MakaoGameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GameStartListener {

    private final MakaoGameRedisRepository gameRedisRepository;
    private final MakaoGameService makaoGameService;

    @RabbitListener(queues = "${makao.amqp.queue.start:makao.start.queue}")
    public void handleGameStart(GameStartMessage message) {
        if (message == null) {
            log.warn("Received null GameStartMessage; skipping");
            return;
        }

        String roomId = message.roomId();
        if (roomId == null || roomId.isBlank()) {
            log.warn("Received GameStartMessage without roomId; skipping");
            return;
        }

        if (gameRedisRepository.existsById(roomId)) {
            log.info("Makao game {} already exists in Redis; skipping creation", roomId);
            return;
        }

        if (message.players() == null || message.players().isEmpty()) {
            log.warn("Cannot create Makao game {} because no players were provided in GameStartMessage", roomId);
            return;
        }

        MakaoGame game = new MakaoGame(roomId, message.players(), message.hostUserId(), message.maxPlayers());

        gameRedisRepository.save(game);
        log.info("Makao game {} created with {} players and persisted to Redis", roomId, message.players().size());

        try {
            makaoGameService.initializeGameAfterStart(roomId);
        } catch (Exception e) {
            log.error("Failed to initialize Makao game {} after creation", roomId, e);
        }
    }
}
