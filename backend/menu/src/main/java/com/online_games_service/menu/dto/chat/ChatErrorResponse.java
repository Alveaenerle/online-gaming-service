package com.online_games_service.menu.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Error response for chat operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatErrorResponse {
    
    private String code;
    private String message;
    private Long retryAfter;  // milliseconds until user can send again (for rate limiting)
    
    public static ChatErrorResponse rateLimited(long retryAfterMs) {
        return ChatErrorResponse.builder()
                .code("RATE_LIMIT")
                .message("You are sending messages too quickly. Please wait.")
                .retryAfter(retryAfterMs)
                .build();
    }
    
    public static ChatErrorResponse notInLobby() {
        return ChatErrorResponse.builder()
                .code("NOT_IN_LOBBY")
                .message("You are not in a lobby.")
                .build();
    }
    
    public static ChatErrorResponse invalidMessage(String reason) {
        return ChatErrorResponse.builder()
                .code("INVALID_MESSAGE")
                .message(reason)
                .build();
    }
}
