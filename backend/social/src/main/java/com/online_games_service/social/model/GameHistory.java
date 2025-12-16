package com.online_games_service.social.model;

import com.online_games_service.common.enums.GameType;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "game_histories")
@Data
@NoArgsConstructor
public class GameHistory {

    @Id
    private String id;

    @Indexed
    private String accountId;

    private GameType gameType;

    private String matchId;

    private boolean isWinner;

    @CreatedDate
    private LocalDateTime playedAt;

    public GameHistory(String accountId, GameType gameType, String matchId, boolean isWinner) {
        this.accountId = accountId;
        this.gameType = gameType;
        this.matchId = matchId;
        this.isWinner = isWinner;
    }
}