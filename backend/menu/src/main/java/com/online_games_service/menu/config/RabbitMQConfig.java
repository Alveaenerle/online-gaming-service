package com.online_games_service.menu.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${menu.amqp.exchange:game.events}")
    private String exchangeName;

    @Value("${menu.amqp.queue.finish.makao:makao.finish.queue}")
    private String makaoFinishQueueName;

    @Value("${menu.amqp.routing.finish.makao:makao.finish}")
    private String makaoFinishRoutingKey;

    @Bean
    public TopicExchange gameEventsExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Queue makaoFinishQueue() {
        return QueueBuilder.durable(makaoFinishQueueName).build();
    }

    @Bean
    public Binding makaoFinishBinding(Queue makaoFinishQueue, TopicExchange gameEventsExchange) {
        return BindingBuilder.bind(makaoFinishQueue).to(gameEventsExchange).with(makaoFinishRoutingKey);
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
