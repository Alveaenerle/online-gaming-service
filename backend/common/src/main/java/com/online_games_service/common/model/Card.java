package com.online_games_service.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.online_games_service.common.enums.CardSuit;
import com.online_games_service.common.enums.CardRank;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Card implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private CardSuit suit;
    private CardRank rank;
}