package com.online_games_service.social.repository;

import com.online_games_service.social.model.SocialProfile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SocialProfileRepository extends MongoRepository<SocialProfile, String> {
}