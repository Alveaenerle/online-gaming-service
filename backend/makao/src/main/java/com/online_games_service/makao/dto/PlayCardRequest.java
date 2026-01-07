package com.online_games_service.makao.dto;

import com.online_games_service.common.enums.CardRank;
import com.online_games_service.common.enums.CardSuit;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PlayCardRequest {
    private CardRank requestRank;
    private CardSuit requestSuit;

    @NotNull
    private CardRank cardRank;

    @NotNull
    private CardSuit cardSuit;
}
