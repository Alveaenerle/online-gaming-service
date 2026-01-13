package com.online_games_service.menu.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for chat history.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistoryResponse {
    
    private List<ChatMessageResponse> messages;
    private int offset;
    private int limit;
    private boolean hasMore;
    private long totalMessages;
}
