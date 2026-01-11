package com.online_games_service.social.config;

import com.online_games_service.common.config.BaseRedisConfig;
import com.online_games_service.social.service.RedisKeyExpirationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@Import(BaseRedisConfig.class)
public class SocialRedisConfig {

    public static final String ONLINE_USER_KEY_PREFIX = "online:user:";
    public static final long PRESENCE_TTL_SECONDS = 35L;
    public static final String KEY_EXPIRATION_CHANNEL = "__keyevent@0__:expired";

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            RedisKeyExpirationListener expirationListener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(expirationListener, new PatternTopic(KEY_EXPIRATION_CHANNEL));
        return container;
    }
}
