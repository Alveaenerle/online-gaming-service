package com.online_games_service.ludo.repository.mongo;
import com.online_games_service.ludo.model.LudoGameResult;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface LudoGameResultRepository extends MongoRepository<LudoGameResult, String> {
}