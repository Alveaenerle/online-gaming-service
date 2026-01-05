package com.online_games_service.makao.dto;

import com.online_games_service.common.model.Card;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerCardView {
    private Card card;
    private boolean playable;
}
