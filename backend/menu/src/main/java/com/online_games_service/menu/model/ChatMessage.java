package com.online_games_service.menu.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Represents a chat message in a lobby.
 * Stored in Redis as JSON in a list keyed by lobby:chat:{lobbyId}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    
    private String id;
    private String lobbyId;
    private String senderId;
    private String senderUsername;
    private String senderAvatar;
    private String content;
    private Instant timestamp;
    private boolean isBlurred;  // For profanity-filtered messages
    private MessageType type;
    
    public enum MessageType {
        USER_MESSAGE,
        SYSTEM_MESSAGE  // For join/leave notifications
    }
}
