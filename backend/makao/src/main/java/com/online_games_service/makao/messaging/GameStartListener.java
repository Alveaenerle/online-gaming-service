package com.online_games_service.makao.messaging;

import com.online_games_service.common.messaging.GameStartMessage;
import com.online_games_service.makao.model.MakaoGame;
import com.online_games_service.makao.repository.redis.MakaoGameRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class GameStartListener {

    private final MakaoGameRedisRepository gameRedisRepository;

    @RabbitListener(queues = "${makao.amqp.queue.start:makao.start.queue}")
    public void handleGameStart(GameStartMessage message) {
        if (message == null) {
            log.warn("Received null GameStartMessage; skipping");
            return;
        }

        String gameId = message.roomId();
        if (gameId == null || gameId.isBlank()) {
            log.warn("Received GameStartMessage without roomId; skipping");
            return;
        }

        if (gameRedisRepository.existsById(gameId)) {
            log.info("Makao game {} already exists in Redis; skipping creation", gameId);
            return;
        }

        List<String> playerOrder = buildPlayerOrder(message.hostUserId(), message.players());
        MakaoGame game = new MakaoGame(gameId, playerOrder);

        gameRedisRepository.save(game);
        log.info("Makao game {} created with {} players and persisted to Redis", gameId, playerOrder.size());
    }

    private List<String> buildPlayerOrder(String hostUserId, Map<String, String> players) {
        List<String> ordered = new ArrayList<>();
        if (hostUserId != null && !hostUserId.isBlank()) {
            ordered.add(hostUserId);
        }
        if (players != null) {
            for (String userId : players.keySet()) {
                if (!Objects.equals(userId, hostUserId)) {
                    ordered.add(userId);
                }
            }
        }
        return ordered;
    }
}
