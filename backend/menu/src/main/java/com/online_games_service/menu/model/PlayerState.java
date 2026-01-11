package com.online_games_service.menu.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerState implements Serializable {
    private static final long serialVersionUID = 1L;

    private String username;
    private boolean ready = false;
    private String avatarId = "avatar_1.png";

    /**
     * Create a default PlayerState for a new player joining a lobby.
     */
    public static PlayerState createDefault(String username) {
        return new PlayerState(username, false, "avatar_1.png");
    }

    /**
     * Toggle the ready status.
     */
    public void toggleReady() {
        this.ready = !this.ready;
    }
}