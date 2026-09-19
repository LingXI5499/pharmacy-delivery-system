package com.pharmacy.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Component @RequiredArgsConstructor
@ConditionalOnProperty(name="app.messaging.enabled",havingValue="true")
public class ReliableEventPublisher {
    private final RabbitTemplate rabbit;
    @ApplicationModuleListener public void on(OrderPaidEvent event){send(RabbitTopologyConfig.EVENTS_EXCHANGE,"order.paid",event,null);}
    @ApplicationModuleListener public void on(OrderPaymentPendingEvent event){send("",RabbitTopologyConfig.ORDER_DELAY_QUEUE,event,expirationMillis(event.deadline()));}
    private void send(String exchange,String key,Object payload,String expiration){
        CorrelationData data=new CorrelationData();rabbit.convertAndSend(exchange,key,payload,message->{
            message.getMessageProperties().setMessageId(data.getId());
            if(expiration!=null) message.getMessageProperties().setExpiration(expiration);
            return message;
        },data);
        try{CorrelationData.Confirm confirm=data.getFuture().get(10,TimeUnit.SECONDS);if(!confirm.isAck())throw new IllegalStateException("RabbitMQ Nack: "+confirm.getReason());}catch(Exception e){throw new IllegalStateException("RabbitMQ publisher confirm failed",e);}
    }
    static String expirationMillis(LocalDateTime deadline){
        if(deadline==null) return "1";
        long millis=Duration.between(LocalDateTime.now(), deadline).toMillis() + 2_000;
        return String.valueOf(Math.max(1L, Math.min(millis, 30L * 60 * 1000)));
    }
}
