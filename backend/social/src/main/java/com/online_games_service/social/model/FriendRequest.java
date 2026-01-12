package com.online_games_service.social.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "friend_requests")
@Data
@NoArgsConstructor
@CompoundIndex(name = "unique_request", def = "{'requesterId': 1, 'addresseeId': 1}", unique = true)
public class FriendRequest {

    public enum Status {
        PENDING,
        ACCEPTED,
        REJECTED
    }

    @Id
    private String id;

    private String requesterId;
    private String requesterUsername;
    private String addresseeId;
    
    private Status status = Status.PENDING;

    @CreatedDate
    private LocalDateTime createdAt;

    public FriendRequest(String requesterId, String requesterUsername, String addresseeId) {
        this.requesterId = requesterId;
        this.requesterUsername = requesterUsername;
        this.addresseeId = addresseeId;
        this.status = Status.PENDING;
    }

    public FriendRequest(String requesterId, String addresseeId) {
        this(requesterId, null, addresseeId);
    }
}