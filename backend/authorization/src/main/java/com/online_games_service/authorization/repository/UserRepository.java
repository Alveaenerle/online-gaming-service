package com.online_games_service.authorization.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.online_games_service.authorization.model.User;

public interface UserRepository extends MongoRepository<User, String>{
    Optional<User> findByUsername(String username);
    Boolean existsByUsername(String username);
}
