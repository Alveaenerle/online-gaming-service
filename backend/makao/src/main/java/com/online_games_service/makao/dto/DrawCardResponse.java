package com.online_games_service.makao.dto;

import com.online_games_service.common.model.Card;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DrawCardResponse {
    private Card drawnCard;
    private boolean playable;
}
