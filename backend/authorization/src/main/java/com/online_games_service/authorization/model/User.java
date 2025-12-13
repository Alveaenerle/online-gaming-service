package com.online_games_service.authorization.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Document(collection = "users")
public class User {
    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    private boolean isGuest = false;

    public User(String username, boolean isGuest) {
        this.username = username;
        this.isGuest = isGuest;
    }
}
