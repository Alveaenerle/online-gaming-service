package com.online_games_service.menu.service.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.online_games_service.menu.dto.chat.ChatHistoryResponse;
import com.online_games_service.menu.dto.chat.ChatMessageResponse;
import com.online_games_service.menu.model.ChatMessage;
import com.online_games_service.menu.model.PlayerState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Service for managing chat messages in Redis.
 * Messages are stored in Redis Lists with automatic TTL matching lobby lifecycle.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final ProfanityFilterService profanityFilter;
    
    private static final String CHAT_KEY_PREFIX = "lobby:chat:";
    private static final Duration CHAT_TTL = Duration.ofHours(1);  // Match room TTL
    private static final int MAX_MESSAGES_PER_LOBBY = 500;
    
    /**
     * Saves a chat message to Redis and returns the processed message.
     * Applies profanity filtering before storage.
     */
    public ChatMessage saveMessage(String lobbyId, String senderId, String senderUsername, 
                                   String senderAvatar, String content) {
        // Apply profanity filter
        var filterResult = profanityFilter.filter(content);
        
        ChatMessage message = ChatMessage.builder()
                .id(UUID.randomUUID().toString())
                .lobbyId(lobbyId)
                .senderId(senderId)
                .senderUsername(senderUsername)
                .senderAvatar(senderAvatar)
                .content(filterResult.filteredMessage())
                .timestamp(Instant.now())
                .isBlurred(filterResult.wasFiltered())
                .type(ChatMessage.MessageType.USER_MESSAGE)
                .build();
        
        String key = CHAT_KEY_PREFIX + lobbyId;
        
        try {
            String messageJson = objectMapper.writeValueAsString(message);
            // Add to the end of the list (newest messages at the end)
            redisTemplate.opsForList().rightPush(key, messageJson);
            
            // Trim to max size (keep newest messages)
            redisTemplate.opsForList().trim(key, -MAX_MESSAGES_PER_LOBBY, -1);
            
            // Refresh TTL
            redisTemplate.expire(key, CHAT_TTL);
            
            log.debug("Saved chat message {} to lobby {}", message.getId(), lobbyId);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize chat message", e);
            throw new RuntimeException("Failed to save chat message", e);
        }
        
        return message;
    }
    
    /**
     * Creates a system message (for join/leave events).
     */
    public ChatMessage createSystemMessage(String lobbyId, String content) {
        ChatMessage message = ChatMessage.builder()
                .id(UUID.randomUUID().toString())
                .lobbyId(lobbyId)
                .senderId("SYSTEM")
                .senderUsername("System")
                .senderAvatar(null)
                .content(content)
                .timestamp(Instant.now())
                .isBlurred(false)
                .type(ChatMessage.MessageType.SYSTEM_MESSAGE)
                .build();
        
        String key = CHAT_KEY_PREFIX + lobbyId;
        
        try {
            String messageJson = objectMapper.writeValueAsString(message);
            redisTemplate.opsForList().rightPush(key, messageJson);
            redisTemplate.expire(key, CHAT_TTL);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize system message", e);
        }
        
        return message;
    }
    
    /**
     * Fetches chat history with pagination.
     * Messages are returned in chronological order (oldest first within the page).
     * Offset 0 = most recent messages, higher offset = older messages.
     */
    public ChatHistoryResponse getChatHistory(String lobbyId, int offset, int limit) {
        String key = CHAT_KEY_PREFIX + lobbyId;
        
        Long totalSize = redisTemplate.opsForList().size(key);
        if (totalSize == null || totalSize == 0) {
            return ChatHistoryResponse.builder()
                    .messages(Collections.emptyList())
                    .offset(offset)
                    .limit(limit)
                    .hasMore(false)
                    .totalMessages(0)
                    .build();
        }
        
        // Calculate range - we want to get messages from the end (newest)
        // For pagination, we go backwards from the end
        long endIndex = totalSize - 1 - offset;
        long startIndex = Math.max(0, endIndex - limit + 1);
        
        if (endIndex < 0) {
            return ChatHistoryResponse.builder()
                    .messages(Collections.emptyList())
                    .offset(offset)
                    .limit(limit)
                    .hasMore(false)
                    .totalMessages(totalSize)
                    .build();
        }
        
        List<Object> rawMessages = redisTemplate.opsForList().range(key, startIndex, endIndex);
        
        if (rawMessages == null || rawMessages.isEmpty()) {
            return ChatHistoryResponse.builder()
                    .messages(Collections.emptyList())
                    .offset(offset)
                    .limit(limit)
                    .hasMore(false)
                    .totalMessages(totalSize)
                    .build();
        }
        
        List<ChatMessageResponse> messages = new ArrayList<>();
        for (Object raw : rawMessages) {
            try {
                ChatMessage msg = objectMapper.readValue(raw.toString(), ChatMessage.class);
                messages.add(ChatMessageResponse.fromChatMessage(msg));
            } catch (JsonProcessingException e) {
                log.warn("Failed to deserialize chat message: {}", raw);
            }
        }
        
        boolean hasMore = startIndex > 0;
        
        return ChatHistoryResponse.builder()
                .messages(messages)
                .offset(offset)
                .limit(limit)
                .hasMore(hasMore)
                .totalMessages(totalSize)
                .build();
    }
    
    /**
     * Deletes all chat messages for a lobby (called when lobby is destroyed).
     */
    public void deleteChatHistory(String lobbyId) {
        String key = CHAT_KEY_PREFIX + lobbyId;
        redisTemplate.delete(key);
        log.debug("Deleted chat history for lobby {}", lobbyId);
    }
    
    /**
     * Gets the avatar for a user from the lobby players map.
     */
    public String getPlayerAvatar(Map<String, PlayerState> players, String userId) {
        PlayerState state = players.get(userId);
        if (state != null && state.getAvatarId() != null) {
            return "/avatars/" + state.getAvatarId();
        }
        return "/avatars/avatar_1.png";  // Default avatar
    }
}
