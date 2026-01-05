package com.online_games_service.makao.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EndGameRequest {
    @NotBlank
    private String roomId;
}
