package com.online_games_service.statistical.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitMQConfig {

    @Value("${statistical.amqp.exchange:game.events}")
    private String exchangeName;

    @Value("${statistical.amqp.queue.game-result:statistical.game-result.queue}")
    private String gameResultQueueName;

    @Value("${statistical.amqp.routing.game-result:*.game.result}")
    private String gameResultRoutingKey;

    @Bean
    public TopicExchange gameEventsExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Queue gameResultQueue() {
        return QueueBuilder.durable(gameResultQueueName).build();
    }

    @Bean
    public Binding gameResultBinding(Queue gameResultQueue, TopicExchange gameEventsExchange) {
        return BindingBuilder.bind(gameResultQueue).to(gameEventsExchange).with(gameResultRoutingKey);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
