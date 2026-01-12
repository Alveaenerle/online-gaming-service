package com.online_games_service.social.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Document(collection = "social_profiles")
@Data
@NoArgsConstructor
public class SocialProfile {

    @Id
    private String id;

    private Set<String> friendIds = new HashSet<>();
    
    // Map of friendId -> friendUsername for displaying names
    private Map<String, String> friendUsernames = new HashMap<>();

    private int friendCount = 0;

    public SocialProfile(String accountId) {
        this.id = accountId;
    }

    public void addFriend(String friendId) {
        this.friendIds.add(friendId);
        updateCount();
    }
    
    public void addFriend(String friendId, String friendUsername) {
        this.friendIds.add(friendId);
        this.friendUsernames.put(friendId, friendUsername);
        updateCount();
    }

    public void removeFriend(String friendId) {
        this.friendIds.remove(friendId);
        this.friendUsernames.remove(friendId);
        updateCount();
    }

    public Set<String> getFriendIds() {
        return Collections.unmodifiableSet(friendIds);
    }
    
    public Map<String, String> getFriendUsernames() {
        return Collections.unmodifiableMap(friendUsernames);
    }
    
    public String getFriendUsername(String friendId) {
        return friendUsernames.getOrDefault(friendId, friendId);
    }

    public void setFriendIds(Set<String> friendIds) {
        this.friendIds = friendIds != null ? friendIds : new HashSet<>();
        updateCount();
    }

    private void updateCount() {
        this.friendCount = this.friendIds.size();
    }
}