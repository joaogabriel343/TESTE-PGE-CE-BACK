package br.gov.pgece.rides.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String QUEUE_NAME    = "rides.queue";
    public static final String EXCHANGE_NAME = "rides.exchange";
    public static final String ROUTING_KEY   = "rides.created";

    @Bean
    public Queue ridesQueue() {
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    public DirectExchange ridesExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public Binding ridesBinding(Queue ridesQueue, DirectExchange ridesExchange) {
        return BindingBuilder.bind(ridesQueue).to(ridesExchange).with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(cf);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
