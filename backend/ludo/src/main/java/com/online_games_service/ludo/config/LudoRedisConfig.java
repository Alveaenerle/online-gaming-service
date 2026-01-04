package com.online_games_service.ludo.config;

import com.online_games_service.common.config.BaseRedisConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@Configuration
@Import(BaseRedisConfig.class) 
@EnableRedisRepositories(basePackages = "com.online_games_service.ludo.repository.redis") 
public class LudoRedisConfig {
}