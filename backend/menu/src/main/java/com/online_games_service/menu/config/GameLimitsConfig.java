package com.online_games_service.menu.config;

import com.online_games_service.common.enums.GameType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "game")
@Data
public class GameLimitsConfig {

    private Limit defaultLimit;

    private Map<GameType, Limit> limits;

    @Data
    public static class Limit {
        private int min;
        private int max;
    }
    
    public Limit getLimitFor(GameType type) {
        if (limits != null && limits.containsKey(type)) {
            return limits.get(type);
        }
        return defaultLimit;
    }
}