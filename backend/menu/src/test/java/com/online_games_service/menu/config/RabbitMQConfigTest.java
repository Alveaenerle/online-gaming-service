package com.online_games_service.menu.config;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class RabbitMQConfigTest {

    @Mock
    private ConnectionFactory connectionFactory;

    private RabbitMQConfig rabbitMQConfig;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        rabbitMQConfig = new RabbitMQConfig();
        
        // Set the @Value properties via reflection
        ReflectionTestUtils.setField(rabbitMQConfig, "exchangeName", "game.events");
        ReflectionTestUtils.setField(rabbitMQConfig, "makaoFinishQueueName", "makao.finish.queue");
        ReflectionTestUtils.setField(rabbitMQConfig, "makaoFinishRoutingKey", "makao.finish");
        ReflectionTestUtils.setField(rabbitMQConfig, "playerLeaveQueueName", "player.leave.queue");
        ReflectionTestUtils.setField(rabbitMQConfig, "playerLeaveRoutingKey", "player.leave");
    }

    @Test
    public void shouldCreateGameEventsExchange() {
        // When
        TopicExchange exchange = rabbitMQConfig.gameEventsExchange();

        // Then
        Assert.assertNotNull(exchange);
        Assert.assertEquals(exchange.getName(), "game.events");
        Assert.assertTrue(exchange.isDurable());
    }

    @Test
    public void shouldCreateMakaoFinishQueue() {
        // When
        Queue queue = rabbitMQConfig.makaoFinishQueue();

        // Then
        Assert.assertNotNull(queue);
        Assert.assertEquals(queue.getName(), "makao.finish.queue");
        Assert.assertTrue(queue.isDurable());
    }

    @Test
    public void shouldCreateMakaoFinishBinding() {
        // Given
        Queue queue = rabbitMQConfig.makaoFinishQueue();
        TopicExchange exchange = rabbitMQConfig.gameEventsExchange();

        // When
        Binding binding = rabbitMQConfig.makaoFinishBinding(queue, exchange);

        // Then
        Assert.assertNotNull(binding);
        Assert.assertEquals(binding.getRoutingKey(), "makao.finish");
    }

    @Test
    public void shouldCreatePlayerLeaveQueue() {
        // When
        Queue queue = rabbitMQConfig.playerLeaveQueue();

        // Then
        Assert.assertNotNull(queue);
        Assert.assertEquals(queue.getName(), "player.leave.queue");
        Assert.assertTrue(queue.isDurable());
    }

    @Test
    public void shouldCreatePlayerLeaveBinding() {
        // Given
        Queue queue = rabbitMQConfig.playerLeaveQueue();
        TopicExchange exchange = rabbitMQConfig.gameEventsExchange();

        // When
        Binding binding = rabbitMQConfig.playerLeaveBinding(queue, exchange);

        // Then
        Assert.assertNotNull(binding);
        Assert.assertEquals(binding.getRoutingKey(), "player.leave");
    }

    @Test
    public void shouldCreateMessageConverter() {
        // When
        MessageConverter converter = rabbitMQConfig.messageConverter();

        // Then
        Assert.assertNotNull(converter);
    }

    @Test
    public void shouldCreateRabbitTemplate() {
        // Given
        MessageConverter converter = rabbitMQConfig.messageConverter();

        // When
        RabbitTemplate template = rabbitMQConfig.rabbitTemplate(connectionFactory, converter);

        // Then
        Assert.assertNotNull(template);
        Assert.assertEquals(template.getMessageConverter(), converter);
    }
}
