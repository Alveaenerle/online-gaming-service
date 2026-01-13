package com.online_games_service.makao.config;

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

    @Value("${makao.amqp.exchange:game.events}")
    private String exchangeName;

    @Value("${makao.amqp.queue.start:makao.start.queue}")
    private String startQueueName;

    @Value("${makao.amqp.routing.start:makao.start}")
    private String startRoutingKey;

    @Value("${makao.amqp.routing.leave:player.leave}")
    private String leaveRoutingKey;

    @Bean
    public TopicExchange gameEventsExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Queue makaoStartQueue() {
        return QueueBuilder.durable(startQueueName).build();
    }

    @Bean
    public Binding makaoStartBinding(Queue makaoStartQueue, TopicExchange gameEventsExchange) {
        return BindingBuilder.bind(makaoStartQueue).to(gameEventsExchange).with(startRoutingKey);
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
