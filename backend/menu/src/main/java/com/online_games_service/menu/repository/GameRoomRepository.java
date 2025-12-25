package com.online_games_service.menu.repository;

import com.online_games_service.common.enums.GameType;
import com.online_games_service.common.enums.RoomStatus;
import com.online_games_service.menu.model.GameRoom;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameRoomRepository extends MongoRepository<GameRoom, String> {
    
    List<GameRoom> findAllByGameType(GameType gameType);
    List<GameRoom> findAllByStatus(RoomStatus status);
    List<GameRoom> findAllByGameTypeAndStatus(GameType gameType, RoomStatus status);
}