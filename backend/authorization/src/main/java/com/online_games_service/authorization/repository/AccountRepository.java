package com.online_games_service.authorization.repository;

import com.online_games_service.authorization.model.Account;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface AccountRepository extends MongoRepository<Account, String> {
    Optional<Account> findByEmail(String email);
    Boolean existsByEmail(String email);
}
