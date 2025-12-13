package com.online_games_service.authorization.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
}
