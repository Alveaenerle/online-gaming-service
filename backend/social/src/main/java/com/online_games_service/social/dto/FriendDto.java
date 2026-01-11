package com.online_games_service.social.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendDto {
    private String id;
    private String username;
    private String status; // ONLINE, OFFLINE, PLAYING
    private String avatarUrl;
}
