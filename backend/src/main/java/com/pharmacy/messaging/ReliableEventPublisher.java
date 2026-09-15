package com.pharmacy.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

@Component @RequiredArgsConstructor
@ConditionalOnProperty(name="app.messaging.enabled",havingValue="true")
public class ReliableEventPublisher {
    private final RabbitTemplate rabbit;
    @ApplicationModuleListener public void on(OrderPaidEvent event){send(RabbitTopologyConfig.EVENTS_EXCHANGE,"order.paid",event);}
    @ApplicationModuleListener public void on(OrderPaymentPendingEvent event){send("",RabbitTopologyConfig.ORDER_DELAY_QUEUE,event);}
    private void send(String exchange,String key,Object payload){
        CorrelationData data=new CorrelationData();rabbit.convertAndSend(exchange,key,payload,message->{message.getMessageProperties().setMessageId(data.getId());return message;},data);
        try{CorrelationData.Confirm confirm=data.getFuture().get(10,TimeUnit.SECONDS);if(!confirm.isAck())throw new IllegalStateException("RabbitMQ Nack: "+confirm.getReason());}catch(Exception e){throw new IllegalStateException("RabbitMQ publisher confirm failed",e);}
    }
}
