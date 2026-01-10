package com.online_games_service.ludo.config;

import com.online_games_service.common.filter.SessionUserFilter;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;

public class LudoConfigTest {

    @Test
    public void redisConfigShouldBeAnnotated() {
        // Given
        Class<LudoRedisConfig> configClass = LudoRedisConfig.class;

        // When & Then
        Assert.assertTrue(configClass.isAnnotationPresent(Configuration.class));
        Assert.assertTrue(configClass.isAnnotationPresent(org.springframework.data.redis.repository.configuration.EnableRedisRepositories.class));
        new LudoRedisConfig();
    }

    @Test
    public void rabbitConfigShouldCreateAllBeans() {
        // Given
        Class<RabbitMQConfig> configClass = RabbitMQConfig.class;
        RabbitMQConfig config = new RabbitMQConfig();

        ReflectionTestUtils.setField(config, "exchangeName", "test-exchange");
        ReflectionTestUtils.setField(config, "startQueueName", "test-queue");
        ReflectionTestUtils.setField(config, "startRoutingKey", "test-routing");

        // When & Then
        Assert.assertTrue(configClass.isAnnotationPresent(Configuration.class));
        Assert.assertTrue(configClass.isAnnotationPresent(org.springframework.amqp.rabbit.annotation.EnableRabbit.class));
        
        TopicExchange exchange = config.gameEventsExchange();
        Queue queue = config.ludoStartQueue();
        MessageConverter converter = config.messageConverter();

        Assert.assertNotNull(exchange);
        Assert.assertNotNull(queue);
        Assert.assertNotNull(converter);

        Assert.assertNotNull(config.ludoStartBinding(queue, exchange));
        Assert.assertNotNull(config.rabbitTemplate(mock(ConnectionFactory.class), converter));
    }
    
    @Test
    public void webSocketConfigShouldBeAnnotated() {
        // Given
        Class<WebSocketConfig> configClass = WebSocketConfig.class;

        // When & Then
        Assert.assertTrue(configClass.isAnnotationPresent(Configuration.class));
        Assert.assertTrue(configClass.isAnnotationPresent(org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker.class));
        new WebSocketConfig();
    }

    @Test
    public void filterConfigShouldCreateBeans() {
        // Given
        FilterConfig config = new FilterConfig();
        SessionUserFilter filter = mock(SessionUserFilter.class);

        // When
        FilterRegistrationBean<SessionUserFilter> bean = config.sessionUserFilterRegistration(filter);

        // Then
        Assert.assertNotNull(bean);
        Assert.assertTrue(FilterConfig.class.isAnnotationPresent(Configuration.class));
    }

    @Test
    public void mongoConfigShouldCreateBeans() {
        // Given
        MongoConfig config = new MongoConfig();
        LocalValidatorFactoryBean factory = mock(LocalValidatorFactoryBean.class);

        // When
        var listener = config.validatingMongoEventListener(factory);
        var validator = config.validator();

        // Then
        Assert.assertNotNull(listener);
        Assert.assertNotNull(validator);
        Assert.assertTrue(MongoConfig.class.isAnnotationPresent(Configuration.class));
    }
}