package com.online_games_service.menu.dto.chat;

import com.online_games_service.menu.model.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response DTO for a chat message sent to clients.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {
    
    private String id;
    private String senderId;
    private String senderUsername;
    private String senderAvatar;
    private String content;
    private Instant timestamp;
    private boolean isBlurred;
    private String type;
    
    public static ChatMessageResponse fromChatMessage(ChatMessage message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .senderId(message.getSenderId())
                .senderUsername(message.getSenderUsername())
                .senderAvatar(message.getSenderAvatar())
                .content(message.getContent())
                .timestamp(message.getTimestamp())
                .isBlurred(message.isBlurred())
                .type(message.getType().name())
                .build();
    }
}
