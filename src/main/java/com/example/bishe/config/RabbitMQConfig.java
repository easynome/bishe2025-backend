package com.example.bishe.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "recommend.exchange";
    public static final String QUEUE_NAME = "recommend.queue";
    public static final String ROUTING_KEY = "recommend.calculate";

    @Bean
    public TopicExchange recommendExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue recommendQueue() {
        return new Queue(QUEUE_NAME);
    }

    @Bean
    public Binding binding(Queue recommendQueue, TopicExchange recommendExchange){
        return BindingBuilder.bind(recommendQueue).to(recommendExchange).with(ROUTING_KEY);
    }
    @Bean
    public MessageConverter jsonMessageConverter(){
        return new Jackson2JsonMessageConverter();
    }
}
