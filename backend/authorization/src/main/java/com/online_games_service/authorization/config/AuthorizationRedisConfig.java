package com.online_games_service.authorization.config;

import com.online_games_service.common.config.BaseRedisConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(BaseRedisConfig.class)
public class AuthorizationRedisConfig {
    // This class imports bean 'redisTemplate' from BaseRedisConfig
}