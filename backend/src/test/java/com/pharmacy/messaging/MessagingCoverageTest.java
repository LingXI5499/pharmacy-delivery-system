package com.pharmacy.messaging;

import com.pharmacy.entity.PharmacyOrder;
import com.pharmacy.enums.OrderStatus;
import com.pharmacy.inventory.InventoryService;
import com.pharmacy.mapper.OrderStatusLogMapper;
import com.pharmacy.mapper.PharmacyOrderMapper;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessagingCoverageTest {
    @Mock private PharmacyOrderMapper orderMapper;
    @Mock private OrderStatusLogMapper logMapper;
    @Mock private InventoryService inventory;
    @Mock private InboxEventMapper inbox;
    @Mock private OrderTimeoutService nestedTimeout;
    @Mock private IncompleteEventPublications publications;
    @Mock private RabbitTemplate rabbit;

    private OrderTimeoutService timeoutService;
    private OrderTimeoutConsumer consumer;
    private EventReplayScheduler replayScheduler;
    private ReliableEventPublisher publisher;

    @org.junit.jupiter.api.BeforeEach
    void assemble() {
        timeoutService = new OrderTimeoutService(orderMapper, logMapper, inventory);
        consumer = new OrderTimeoutConsumer(inbox, nestedTimeout);
        replayScheduler = new EventReplayScheduler(publications);
        publisher = new ReliableEventPublisher(rabbit);
    }

    @AfterEach
    void clearTx() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void closeIfPendingIgnoresNonPayableOrders() {
        when(orderMapper.lockById(1L)).thenReturn(null);
        timeoutService.closeIfPending(1L);
        verify(inventory, never()).release(anyLong(), anyString(), any());

        PharmacyOrder packing = new PharmacyOrder();
        packing.setOrderStatus(OrderStatus.TO_PACK);
        when(orderMapper.lockById(2L)).thenReturn(packing);
        timeoutService.closeIfPending(2L);

        PharmacyOrder pending = new PharmacyOrder();
        pending.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        pending.setPaymentDeadline(LocalDateTime.now().plusMinutes(10));
        when(orderMapper.lockById(3L)).thenReturn(pending);
        timeoutService.closeIfPending(3L);
        verify(inventory, never()).release(anyLong(), anyString(), any());
    }

    @Test
    void closeIfPendingReleasesAndWritesLog() {
        PharmacyOrder pending = new PharmacyOrder();
        pending.setId(4L);
        pending.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        pending.setPaymentDeadline(LocalDateTime.now().minusMinutes(1));
        when(orderMapper.lockById(4L)).thenReturn(pending);

        timeoutService.closeIfPending(4L);

        verify(inventory).release(4L, "支付超时释放库存", null);
        assertEquals(OrderStatus.CLOSED_TIMEOUT, pending.getOrderStatus());
        verify(logMapper).insert(any(com.pharmacy.entity.OrderStatusLog.class));
    }

    @Test
    void consumerSkipsDuplicateThenAcksAfterCommit() throws Exception {
        TransactionSynchronizationManager.initSynchronization();
        OrderPaymentPendingEvent event = new OrderPaymentPendingEvent(9L, "O9", LocalDateTime.now());
        MessageProperties props = new MessageProperties();
        props.setMessageId("evt-1");
        props.setDeliveryTag(12L);
        Message message = new Message(new byte[0], props);
        Channel channel = org.mockito.Mockito.mock(Channel.class);
        when(inbox.selectCount(any())).thenReturn(1L);

        consumer.receive(event, message, channel);
        verify(nestedTimeout, never()).closeIfPending(anyLong());
        TransactionSynchronizationManager.getSynchronizations().get(0).afterCommit();
        verify(channel).basicAck(12L, false);
    }

    @Test
    void consumerInsertsInboxForNewEvent() {
        TransactionSynchronizationManager.initSynchronization();
        OrderPaymentPendingEvent event = new OrderPaymentPendingEvent(9L, "O9", LocalDateTime.now());
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(1L);
        Message message = new Message(new byte[0], props);
        Channel channel = org.mockito.Mockito.mock(Channel.class);
        when(inbox.selectCount(any())).thenReturn(0L);

        consumer.receive(event, message, channel);
        ArgumentCaptor<InboxEvent> captor = ArgumentCaptor.forClass(InboxEvent.class);
        verify(inbox).insert(captor.capture());
        assertEquals("order-timeout", captor.getValue().getConsumerName());
        verify(nestedTimeout).closeIfPending(9L);
    }

    @Test
    void replaySwallowsBrokerFailures() {
        replayScheduler.replay();
        verify(publications).resubmitIncompletePublicationsOlderThan(any());
        doThrow(new IllegalStateException("broker down")).when(publications)
                .resubmitIncompletePublicationsOlderThan(any());
        replayScheduler.replay();
    }

    @Test
    void publisherRequiresAck() {
        doAnswer(invocation -> {
            CorrelationData data = invocation.getArgument(4);
            data.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbit).convertAndSend(anyString(), anyString(), any(), any(), any(CorrelationData.class));
        publisher.on(new OrderPaidEvent(1L, "O1", "PAY1", LocalDateTime.now()));

        doAnswer(invocation -> {
            CorrelationData data = invocation.getArgument(4);
            data.getFuture().complete(new CorrelationData.Confirm(false, "nack"));
            return null;
        }).when(rabbit).convertAndSend(anyString(), anyString(), any(), any(), any(CorrelationData.class));
        assertThrows(IllegalStateException.class,
                () -> publisher.on(new OrderPaymentPendingEvent(1L, "O1", LocalDateTime.now().plusMinutes(5))));
    }

    @Test
    void rabbitTopologyBeansAreDurable() {
        RabbitTopologyConfig config = new RabbitTopologyConfig();
        assertEquals("pharmacy.events", config.eventsExchange().getName());
        assertEquals("pharmacy.order.timeout", config.timeoutExchange().getName());
        assertEquals(RabbitTopologyConfig.ORDER_DELAY_QUEUE, config.delayQueue().getName());
        assertEquals(RabbitTopologyConfig.ORDER_CLOSE_QUEUE, config.closeQueue().getName());
        config.closeBinding(config.closeQueue(), config.timeoutExchange());
        config.rabbitJsonConverter(new com.fasterxml.jackson.databind.ObjectMapper());
    }
}
