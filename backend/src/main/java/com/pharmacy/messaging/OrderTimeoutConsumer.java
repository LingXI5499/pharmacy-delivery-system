package com.pharmacy.messaging;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.LocalDateTime;

@Component @RequiredArgsConstructor
@ConditionalOnProperty(name="app.messaging.enabled",havingValue="true")
public class OrderTimeoutConsumer {
    private final InboxEventMapper inbox; private final OrderTimeoutService service;
    @RabbitListener(queues=RabbitTopologyConfig.ORDER_CLOSE_QUEUE)
    @Transactional
    public void receive(OrderPaymentPendingEvent event, Message message, Channel channel){
        String eventId=message.getMessageProperties().getMessageId();if(eventId==null)eventId=event.orderId()+":"+event.deadline();
        boolean duplicate=inbox.selectCount(new LambdaQueryWrapper<InboxEvent>().eq(InboxEvent::getConsumerName,"order-timeout").eq(InboxEvent::getEventId,eventId))>0;
        if(!duplicate){InboxEvent row=new InboxEvent();row.setConsumerName("order-timeout");row.setEventId(eventId);row.setEventType(OrderPaymentPendingEvent.class.getName());row.setProcessedTime(LocalDateTime.now());inbox.insert(row);service.closeIfPending(event.orderId());}
        long tag=message.getMessageProperties().getDeliveryTag();TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCommit(){try{channel.basicAck(tag,false);}catch(Exception e){throw new IllegalStateException(e);}}});
    }
}
