package com.online_games_service.social.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${websocket.allowed-origins:http://localhost}")
    private String allowedOrigins;

    private final RedisTemplate<String, Object> sessionRedisTemplate;

    public WebSocketConfig(@Qualifier("sessionReadRedisTemplate") RedisTemplate<String, Object> sessionRedisTemplate) {
        this.sessionRedisTemplate = sessionRedisTemplate;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/presence")
                .setAllowedOriginPatterns(allowedOrigins.split(","))
                .addInterceptors(new WebSocketAuthInterceptor(sessionRedisTemplate))
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new WebSocketUserInterceptor());
    }
}
