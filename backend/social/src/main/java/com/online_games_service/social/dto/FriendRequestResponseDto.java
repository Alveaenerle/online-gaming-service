package com.online_games_service.social.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequestResponseDto {
    private String requestId;
    private String requesterId;
    private String addresseeId;
    private String status;
    private String message;
}
