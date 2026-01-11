package com.online_games_service.social.repository;

import com.online_games_service.social.model.FriendRequest;
import com.online_games_service.social.model.FriendRequest.Status;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRequestRepository extends MongoRepository<FriendRequest, String> {

    boolean existsByRequesterIdAndAddresseeId(String requesterId, String addresseeId);

    Optional<FriendRequest> findByRequesterIdAndAddresseeId(String requesterId, String addresseeId);

    List<FriendRequest> findAllByRequesterId(String requesterId);

    List<FriendRequest> findAllByAddresseeId(String addresseeId);
    
    List<FriendRequest> findAllByAddresseeIdAndStatus(String addresseeId, Status status);
    
    List<FriendRequest> findAllByRequesterIdAndStatus(String requesterId, Status status);
    
    boolean existsByRequesterIdAndAddresseeIdAndStatus(String requesterId, String addresseeId, Status status);
    
    Optional<FriendRequest> findByIdAndAddresseeId(String id, String addresseeId);
    
    void deleteByRequesterIdAndAddresseeIdAndStatus(String requesterId, String addresseeId, Status status);
}