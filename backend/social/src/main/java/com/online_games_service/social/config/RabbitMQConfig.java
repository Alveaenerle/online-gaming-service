package com.online_games_service.social.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for listening to game start events.
 * When a game starts (makao or ludo), all pending invites for that lobby are deleted.
 */
@Configuration
public class RabbitMQConfig {

    public static final String GAME_EVENTS_EXCHANGE = "game.events";
    public static final String GAME_START_QUEUE = "social.game-start.queue";
    public static final String GAME_START_ROUTING_PATTERN = "*.start";

    /**
     * Creates or binds to the existing game.events topic exchange.
     */
    @Bean
    public TopicExchange gameEventsExchange() {
        return new TopicExchange(GAME_EVENTS_EXCHANGE, true, false);
    }

    /**
     * Queue for receiving game start events in the social service.
     */
    @Bean
    public Queue gameStartQueue() {
        return QueueBuilder.durable(GAME_START_QUEUE).build();
    }

    /**
     * Binding that routes all *.start messages (makao.start, ludo.start) to our queue.
     */
    @Bean
    public Binding gameStartBinding(Queue gameStartQueue, TopicExchange gameEventsExchange) {
        return BindingBuilder.bind(gameStartQueue)
                .to(gameEventsExchange)
                .with(GAME_START_ROUTING_PATTERN);
    }

    /**
     * JSON message converter for RabbitMQ messages.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
