package com.online_games_service.menu.messaging;

import com.online_games_service.common.enums.GameType;
import com.online_games_service.common.messaging.GameStartMessage;
import com.online_games_service.menu.model.GameRoom;
import com.online_games_service.menu.model.PlayerState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class GameStartPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final TopicExchange gameEventsExchange;

    @Value("${menu.amqp.routing.start.makao:makao.start}")
    private String makaoRoutingKey;

    @Value("${menu.amqp.routing.start.ludo:ludo.start}")
    private String ludoRoutingKey;

    public void publish(GameRoom room) {
        String routingKey = routingKeyFor(room.getGameType());
        if (routingKey == null) {
            log.warn("No routing key configured for gameType {}. Skipping publish for room {}.", room.getGameType(), room.getId());
            return;
        }

        // Convert players map from Map<String, PlayerState> to Map<String, String>
        Map<String, String> playerUsernames = new HashMap<>();
        for (Map.Entry<String, PlayerState> entry : room.getPlayers().entrySet()) {
            playerUsernames.put(entry.getKey(), entry.getValue().getUsername());
        }

        GameStartMessage payload = new GameStartMessage(
                room.getId(),
                room.getName(),
                room.getGameType(),
                playerUsernames,
                room.getMaxPlayers(),
                room.getHostUserId(),
                room.getHostUsername()
        );

        try {
            rabbitTemplate.convertAndSend(gameEventsExchange.getName(), routingKey, payload);
            log.info("Published game start for room {} to exchange {} with routing {}", room.getId(), gameEventsExchange.getName(), routingKey);
        } catch (AmqpException ex) {
            log.error("Failed to publish game start for room {} to exchange {} with routing {}", room.getId(), gameEventsExchange.getName(), routingKey, ex);
            throw new IllegalStateException("Failed to publish game start event", ex);
        }
    }

    private String routingKeyFor(GameType gameType) {
        if (gameType == null) {
            return null;
        }
        return switch (gameType) {
            case MAKAO -> makaoRoutingKey;
            case LUDO -> ludoRoutingKey;
            default -> null;
        };
    }
}
