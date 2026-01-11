package com.online_games_service.social.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequestResponseDto {
    private String id;
    private String requesterId;
    private String requesterUsername;
    private String addresseeId;
    private String status;
    @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private java.time.LocalDateTime createdAt;
    private String message;
}
