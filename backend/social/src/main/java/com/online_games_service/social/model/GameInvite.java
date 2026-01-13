package com.online_games_service.social.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * Represents a game invite from one user to another.
 * Stored in Redis as game invites are temporary and exist only while a lobby is waiting.
 * When the game starts, all invites for that lobby are automatically deleted via RabbitMQ event.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameInvite implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Unique identifier for this invite.
     */
    private String id;

    /**
     * The user ID of the person sending the invite.
     */
    private String senderId;

    /**
     * The username of the sender (for display purposes).
     */
    private String senderUsername;

    /**
     * The user ID of the person receiving the invite.
     */
    private String targetId;

    /**
     * The ID of the game lobby/room the invite is for.
     */
    private String lobbyId;

    /**
     * The name of the lobby (for display purposes).
     */
    private String lobbyName;

    /**
     * The type of game (e.g., MAKAO, LUDO).
     */
    private String gameType;

    /**
     * The access code for the room, used to join the game.
     */
    private String accessCode;

    /**
     * Timestamp when the invite was created (epoch millis for Redis serialization).
     */
    private long createdAt;

    /**
     * Create a new game invite with auto-generated ID and timestamp.
     */
    public static GameInvite create(String senderId, String senderUsername, String targetId,
                                     String lobbyId, String lobbyName, String gameType, String accessCode) {
        return GameInvite.builder()
                .id(java.util.UUID.randomUUID().toString())
                .senderId(senderId)
                .senderUsername(senderUsername)
                .targetId(targetId)
                .lobbyId(lobbyId)
                .lobbyName(lobbyName)
                .gameType(gameType)
                .accessCode(accessCode)
                .createdAt(System.currentTimeMillis())
                .build();
    }

    /**
     * Get createdAt as Instant for display.
     */
    @JsonIgnore
    public Instant getCreatedAtInstant() {
        return Instant.ofEpochMilli(createdAt);
    }
}
