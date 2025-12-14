package com.online_games_service.social.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashSet;
import java.util.Set;

@Document(collection = "social_profiles")
@Data
@NoArgsConstructor
public class SocialProfile {

    @Id
    private String id;

    private Set<String> friendIds = new HashSet<>();

    private int friendCount = 0;

    public SocialProfile(String accountId) {
        this.id = accountId;
    }
    
    public void addFriend(String friendId) {
        this.friendIds.add(friendId);
        this.friendCount = this.friendIds.size();
    }

    public void removeFriend(String friendId) {
        this.friendIds.remove(friendId);
        this.friendCount = this.friendIds.size();
    }
}