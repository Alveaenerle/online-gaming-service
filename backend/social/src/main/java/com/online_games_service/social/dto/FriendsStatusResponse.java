package com.online_games_service.social.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendsStatusResponse {
    private List<UserPresenceStatus> friends;
}
