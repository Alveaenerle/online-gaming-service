package com.online_games_service.menu.config;

import com.online_games_service.common.enums.GameType;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

public class GameLimitsConfigTest {

    @Test
    public void shouldReturnSpecificLimitWhenConfigured() {
        GameLimitsConfig config = new GameLimitsConfig();
        GameLimitsConfig.Limit defaultLimit = new GameLimitsConfig.Limit();
        defaultLimit.setMin(2);
        defaultLimit.setMax(4);
        config.setDefaultLimit(defaultLimit);

        GameLimitsConfig.Limit ludoLimit = new GameLimitsConfig.Limit();
        ludoLimit.setMin(3);
        ludoLimit.setMax(6);

        Map<GameType, GameLimitsConfig.Limit> limits = new HashMap<>();
        limits.put(GameType.LUDO, ludoLimit);
        config.setLimits(limits);

        GameLimitsConfig.Limit result = config.getLimitFor(GameType.LUDO);

        Assert.assertEquals(result.getMin(), 3);
        Assert.assertEquals(result.getMax(), 6);
    }

    @Test
    public void shouldFallbackToDefaultWhenLimitMissing() {
        GameLimitsConfig config = new GameLimitsConfig();
        GameLimitsConfig.Limit defaultLimit = new GameLimitsConfig.Limit();
        defaultLimit.setMin(2);
        defaultLimit.setMax(5);
        config.setDefaultLimit(defaultLimit);

        Map<GameType, GameLimitsConfig.Limit> limits = new HashMap<>();
        config.setLimits(limits);

        GameLimitsConfig.Limit result = config.getLimitFor(GameType.MAKAO);

        Assert.assertEquals(result.getMin(), 2);
        Assert.assertEquals(result.getMax(), 5);
    }

    @Test
    public void shouldFallbackToDefaultWhenLimitsMapIsNull() {
        GameLimitsConfig config = new GameLimitsConfig();
        GameLimitsConfig.Limit defaultLimit = new GameLimitsConfig.Limit();
        defaultLimit.setMin(1);
        defaultLimit.setMax(4);
        config.setDefaultLimit(defaultLimit);

        config.setLimits(null);

        GameLimitsConfig.Limit result = config.getLimitFor(GameType.MAKAO);

        Assert.assertEquals(result.getMin(), 1);
        Assert.assertEquals(result.getMax(), 4);
    }
}
