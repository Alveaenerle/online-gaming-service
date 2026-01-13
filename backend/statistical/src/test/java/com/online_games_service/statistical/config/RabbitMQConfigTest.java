package com.online_games_service.statistical.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Unit tests for RabbitMQConfig.
 */
public class RabbitMQConfigTest {

    private RabbitMQConfig config;

    @BeforeMethod
    public void setUp() {
        config = new RabbitMQConfig();
        ReflectionTestUtils.setField(config, "exchangeName", "game.events");
        ReflectionTestUtils.setField(config, "gameResultQueueName", "statistical.game-result.queue");
        ReflectionTestUtils.setField(config, "gameResultRoutingKey", "*.game.result");
    }

    @Test
    public void gameEventsExchange_createsTopicExchange() {
        TopicExchange exchange = config.gameEventsExchange();

        assertNotNull(exchange);
        assertEquals(exchange.getName(), "game.events");
        assertTrue(exchange.isDurable());
        assertFalse(exchange.isAutoDelete());
    }

    @Test
    public void gameResultQueue_createsDurableQueue() {
        Queue queue = config.gameResultQueue();

        assertNotNull(queue);
        assertEquals(queue.getName(), "statistical.game-result.queue");
        assertTrue(queue.isDurable());
    }

    @Test
    public void gameResultBinding_bindsQueueToExchange() {
        Queue queue = config.gameResultQueue();
        TopicExchange exchange = config.gameEventsExchange();

        Binding binding = config.gameResultBinding(queue, exchange);

        assertNotNull(binding);
        assertEquals(binding.getExchange(), "game.events");
        assertEquals(binding.getDestination(), "statistical.game-result.queue");
        assertEquals(binding.getRoutingKey(), "*.game.result");
    }

    @Test
    public void messageConverter_createsJacksonConverter() {
        MessageConverter converter = config.messageConverter();

        assertNotNull(converter);
        assertTrue(converter instanceof Jackson2JsonMessageConverter);
    }

    @Test
    public void rabbitTemplate_usesMessageConverter() {
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        MessageConverter messageConverter = config.messageConverter();

        RabbitTemplate template = config.rabbitTemplate(connectionFactory, messageConverter);

        assertNotNull(template);
        assertEquals(template.getMessageConverter(), messageConverter);
    }
}
