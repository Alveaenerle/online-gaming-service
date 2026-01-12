package com.online_games_service.menu.controller;

import com.online_games_service.menu.config.WebSocketUserInterceptor.WebSocketPrincipal;
import com.online_games_service.menu.dto.chat.*;
import com.online_games_service.menu.model.ChatMessage;
import com.online_games_service.menu.model.GameRoom;
import com.online_games_service.menu.service.chat.ChatRateLimiterService;
import com.online_games_service.menu.service.chat.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Optional;

/**
 * WebSocket controller for lobby chat functionality.
 * Handles chat messages, typing indicators, and chat history requests.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final ChatRateLimiterService rateLimiter;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String KEY_ROOM = "game:room:";
    private static final String KEY_USER_ROOM_BY_ID = "game:user-room:id:";

    /**
     * Handles sending a chat message.
     * Rate limiting and profanity filtering are applied.
     */
    @MessageMapping("/chat/{lobbyId}/send")
    public void sendMessage(@DestinationVariable String lobbyId, 
                           @Payload ChatMessageRequest request, 
                           Principal principal) {
        if (principal == null) {
            log.warn("Received chat message without authenticated principal");
            return;
        }

        String userId = principal.getName();
        String username = getUsername(principal);

        // Verify user is in the lobby
        String userLobbyId = (String) redisTemplate.opsForValue().get(KEY_USER_ROOM_BY_ID + userId);
        if (userLobbyId == null || !userLobbyId.equals(lobbyId)) {
            messagingTemplate.convertAndSendToUser(
                    userId, "/queue/chat/error", ChatErrorResponse.notInLobby());
            return;
        }

        // Check rate limit
        Optional<Long> rateLimited = rateLimiter.checkRateLimit(userId);
        if (rateLimited.isPresent()) {
            messagingTemplate.convertAndSendToUser(
                    userId, "/queue/chat/error", ChatErrorResponse.rateLimited(rateLimited.get()));
            return;
        }

        // Validate message content
        if (request.getContent() == null || request.getContent().isBlank()) {
            messagingTemplate.convertAndSendToUser(
                    userId, "/queue/chat/error", ChatErrorResponse.invalidMessage("Message cannot be empty"));
            return;
        }

        if (request.getContent().length() > 500) {
            messagingTemplate.convertAndSendToUser(
                    userId, "/queue/chat/error", ChatErrorResponse.invalidMessage("Message too long (max 500 chars)"));
            return;
        }

        // Get user's avatar from the room
        GameRoom room = (GameRoom) redisTemplate.opsForValue().get(KEY_ROOM + lobbyId);
        String avatar = room != null ? chatService.getPlayerAvatar(room.getPlayers(), userId) : "/avatars/avatar_1.png";

        // Save and broadcast message (profanity filter is applied in ChatService)
        ChatMessage message = chatService.saveMessage(lobbyId, userId, username, avatar, request.getContent());
        
        ChatMessageResponse response = ChatMessageResponse.fromChatMessage(message);
        messagingTemplate.convertAndSend("/topic/room/" + lobbyId + "/chat", response);
        
        log.debug("Chat message sent in lobby {} by {}", lobbyId, username);
    }

    /**
     * Handles typing indicator events (start/stop).
     * These are ephemeral and not stored.
     */
    @MessageMapping("/chat/{lobbyId}/typing")
    public void handleTyping(@DestinationVariable String lobbyId,
                            @Payload TypingIndicator indicator,
                            Principal principal) {
        if (principal == null) return;

        String userId = principal.getName();
        String username = getUsername(principal);

        // Verify user is in the lobby
        String userLobbyId = (String) redisTemplate.opsForValue().get(KEY_USER_ROOM_BY_ID + userId);
        if (userLobbyId == null || !userLobbyId.equals(lobbyId)) {
            return;
        }

        // Broadcast typing indicator to other users
        TypingIndicator broadcast = TypingIndicator.builder()
                .userId(userId)
                .username(username)
                .isTyping(indicator.isTyping())
                .build();

        messagingTemplate.convertAndSend("/topic/room/" + lobbyId + "/typing", broadcast);
    }

    /**
     * Handles request for chat history with pagination.
     */
    @MessageMapping("/chat/{lobbyId}/history")
    @SendToUser("/queue/chat/history")
    public ChatHistoryResponse getChatHistory(@DestinationVariable String lobbyId,
                                              @Payload ChatHistoryRequest request,
                                              Principal principal) {
        if (principal == null) {
            log.warn("Received history request without authenticated principal");
            return ChatHistoryResponse.builder().build();
        }

        String userId = principal.getName();

        // Verify user is in the lobby
        String userLobbyId = (String) redisTemplate.opsForValue().get(KEY_USER_ROOM_BY_ID + userId);
        if (userLobbyId == null || !userLobbyId.equals(lobbyId)) {
            return ChatHistoryResponse.builder().build();
        }

        int offset = Math.max(0, request.getOffset());
        int limit = Math.min(Math.max(1, request.getLimit()), 100);

        return chatService.getChatHistory(lobbyId, offset, limit);
    }

    private String getUsername(Principal principal) {
        if (principal instanceof WebSocketPrincipal wsPrincipal) {
            return wsPrincipal.getUsername();
        }
        return "Unknown";
    }
}
