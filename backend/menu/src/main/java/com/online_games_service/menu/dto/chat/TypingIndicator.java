package com.online_games_service.menu.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for typing indicator events (ephemeral, not stored).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TypingIndicator {
    
    private String userId;
    private String username;
    private boolean isTyping;
}
