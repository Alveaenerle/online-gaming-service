package com.online_games_service.menu.service;

import com.online_games_service.common.enums.GameType;
import com.online_games_service.common.enums.RoomStatus;
import com.online_games_service.menu.config.GameLimitsConfig;
import com.online_games_service.menu.dto.CreateRoomRequest;
import com.online_games_service.menu.model.GameRoom;
import com.online_games_service.menu.repository.GameRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GameRoomService {

    private final GameRoomRepository gameRoomRepository;
    private final GameLimitsConfig gameLimitsConfig;

    @Transactional
    public GameRoom createRoom(CreateRoomRequest request, String hostUsername) {
        GameLimitsConfig.Limit limit = gameLimitsConfig.getLimitFor(request.getGameType());

        validatePlayerLimits(request.getMaxPlayers(), limit);

        GameRoom newRoom = new GameRoom(
                request.getName(),
                request.getGameType(),
                hostUsername,
                request.getMaxPlayers(),
                request.isPrivate()
        );

        if (request.isPrivate()) {
            newRoom.setAccessCode(generateAccessCode());
        }

        return gameRoomRepository.save(newRoom);
    }

    public List<GameRoom> getWaitingRooms(GameType gameType) {
        if (gameType != null) {
            return gameRoomRepository.findAllByGameTypeAndStatus(gameType, RoomStatus.WAITING);
        }
        return gameRoomRepository.findAllByStatus(RoomStatus.WAITING);
    }

    private void validatePlayerLimits(int requestedMaxPlayers, GameLimitsConfig.Limit limit) {
        if (limit == null) {
            return;
        }
        if (requestedMaxPlayers < limit.getMin() || requestedMaxPlayers > limit.getMax()) {
            throw new IllegalArgumentException(
                String.format("For this game, the number of players must be between %d and %d", 
                limit.getMin(), limit.getMax())
            );
        }
    }

    private String generateAccessCode() {
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}