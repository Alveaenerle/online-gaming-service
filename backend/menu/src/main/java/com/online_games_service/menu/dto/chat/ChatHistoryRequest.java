package com.online_games_service.menu.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for fetching chat history with pagination.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistoryRequest {
    
    private int offset = 0;
    private int limit = 50;
}
