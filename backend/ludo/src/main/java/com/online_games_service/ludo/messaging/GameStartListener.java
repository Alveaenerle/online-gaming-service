package com.online_games_service.ludo.messaging;

import com.online_games_service.common.messaging.GameStartMessage;
import com.online_games_service.ludo.repository.redis.LudoGameRedisRepository;
import com.online_games_service.ludo.service.LudoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class GameStartListener {

    private final LudoGameRedisRepository gameRedisRepository;
    private final LudoService ludoService;

    @RabbitListener(queues = "${ludo.amqp.queue.start:ludo.start.queue}")
    public void handleGameStart(GameStartMessage message) {
        if (message == null) {
            log.warn("Received null msg");
            return;
        }

        String roomId = message.roomId();

        if (gameRedisRepository.existsById(roomId)) {
            log.info("Game {} already exists", roomId);
            return;
        }

        List<String> playerIds = buildPlayerOrder(message.hostUserId(), message.players());

        ludoService.createGame(
                roomId,
                playerIds,
                message.hostUserId(),
                message.players(),
                message.playerAvatars(),
                message.maxPlayers()
        );

        log.info("Created Ludo game {}", roomId);
    }

    private List<String> buildPlayerOrder(String hostUserId, Map<String, String> players) {
        List<String> ordered = new ArrayList<>();
        if (hostUserId != null && players.containsKey(hostUserId)) {
            ordered.add(hostUserId);
        }
        if (players != null) {
            players.keySet().stream()
                    .filter(id -> !id.equals(hostUserId))
                    .sorted()
                    .forEach(ordered::add);
        }
        return ordered;
    }
}