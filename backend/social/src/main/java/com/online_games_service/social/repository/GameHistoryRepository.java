package com.online_games_service.social.repository;

import com.online_games_service.common.enums.GameType;
import com.online_games_service.social.model.GameHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameHistoryRepository extends MongoRepository<GameHistory, String> {

    List<GameHistory> findAllByAccountId(String accountId);

    List<GameHistory> findAllByAccountIdAndGameType(String accountId, GameType gameType);
}