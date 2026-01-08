package com.online_games_service.ludo.config;

import org.testng.Assert;
import org.testng.annotations.Test;

public class LudoConfigTest {

    @Test
    public void redisConfigShouldBeAnnotated() {
        // Given
        Class<LudoRedisConfig> configClass = LudoRedisConfig.class;

        // When & Then
        Assert.assertTrue(configClass.isAnnotationPresent(org.springframework.context.annotation.Configuration.class));
        Assert.assertTrue(configClass.isAnnotationPresent(org.springframework.data.redis.repository.configuration.EnableRedisRepositories.class));
    }

    @Test
    public void rabbitConfigShouldBeAnnotated() {
        // Given
        Class<RabbitMQConfig> configClass = RabbitMQConfig.class;

        // When & Then
        Assert.assertTrue(configClass.isAnnotationPresent(org.springframework.context.annotation.Configuration.class));
        Assert.assertTrue(configClass.isAnnotationPresent(org.springframework.amqp.rabbit.annotation.EnableRabbit.class));
    }
    
    @Test
    public void webSocketConfigShouldBeAnnotated() {
        // Given
        Class<WebSocketConfig> configClass = WebSocketConfig.class;

        // When & Then
        Assert.assertTrue(configClass.isAnnotationPresent(org.springframework.context.annotation.Configuration.class));
        Assert.assertTrue(configClass.isAnnotationPresent(org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker.class));
    }
}