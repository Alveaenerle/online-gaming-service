package com.online_games_service.authorization.repository;

import com.online_games_service.authorization.model.Account;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AccountRepository extends MongoRepository<Account, String> {
    Optional<Account> findByEmail(String email);
    Optional<Account> findByUserId(String userId);
    Boolean existsByEmail(String email);
}
