package com.pharmacy.messaging;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class RabbitTopologyConfig {
    public static final String EVENTS_EXCHANGE="pharmacy.events";
    public static final String ORDER_TIMEOUT_EXCHANGE="pharmacy.order.timeout";
    public static final String ORDER_DELAY_QUEUE="pharmacy.order.payment.delay";
    public static final String ORDER_CLOSE_QUEUE="pharmacy.order.close";
    @Bean TopicExchange eventsExchange(){return new TopicExchange(EVENTS_EXCHANGE,true,false);}
    @Bean DirectExchange timeoutExchange(){return new DirectExchange(ORDER_TIMEOUT_EXCHANGE,true,false);}
    @Bean Queue delayQueue(){return QueueBuilder.durable(ORDER_DELAY_QUEUE).ttl(30*60*1000).deadLetterExchange(ORDER_TIMEOUT_EXCHANGE).deadLetterRoutingKey("order.close").build();}
    @Bean Queue closeQueue(){return QueueBuilder.durable(ORDER_CLOSE_QUEUE).build();}
    @Bean Binding closeBinding(Queue closeQueue,DirectExchange timeoutExchange){return BindingBuilder.bind(closeQueue).to(timeoutExchange).with("order.close");}
    @Bean Jackson2JsonMessageConverter rabbitJsonConverter(ObjectMapper mapper){return new Jackson2JsonMessageConverter(mapper);}
}
