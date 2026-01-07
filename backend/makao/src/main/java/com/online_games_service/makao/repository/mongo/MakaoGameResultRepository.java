package com.online_games_service.makao.repository.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.online_games_service.makao.model.MakaoGameResult;

public interface MakaoGameResultRepository extends MongoRepository<MakaoGameResult, String> {
}
