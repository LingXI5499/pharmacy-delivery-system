package com.pharmacy.payment;

import com.pharmacy.common.ErrorCode;
import com.pharmacy.entity.PharmacyOrder;
import com.pharmacy.enums.OrderStatus;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.inventory.InventoryService;
import com.pharmacy.mapper.OrderStatusLogMapper;
import com.pharmacy.mapper.PharmacyOrderMapper;
import com.pharmacy.messaging.OrderPaidEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    @Mock private PaymentAttemptMapper paymentMapper;
    @Mock private PharmacyOrderMapper orderMapper;
    @Mock private OrderStatusLogMapper logMapper;
    @Mock private InventoryService inventoryService;
    @Mock private ApplicationEventPublisher events;
    @InjectMocks private PaymentService paymentService;

    @Test
    void nonOwnerCannotCreatePaymentAttempt() {
        PharmacyOrder order = order(1L, 9L, OrderStatus.PENDING_PAYMENT);
        when(orderMapper.lockById(1L)).thenReturn(order);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> paymentService.createAttempt(99L, 1L));
        assertEquals(ErrorCode.NOT_FOUND, ex.getCode());
        verify(paymentMapper, never()).insert(any(PaymentAttempt.class));
    }

    @Test
    void callbackRejectsMissingPayment() {
        when(paymentMapper.lockByNo("missing")).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> paymentService.callback(1L, "missing", "cb-1", true, BigDecimal.TEN));
        assertEquals(ErrorCode.NOT_FOUND, ex.getCode());
    }

    @Test
    void callbackRejectsAmountMismatch() {
        PaymentAttempt attempt = pendingAttempt(5L, 1L, new BigDecimal("10.00"));
        PharmacyOrder order = order(1L, 8L, OrderStatus.PENDING_PAYMENT);
        when(paymentMapper.lockByNo("PAY1")).thenReturn(attempt);
        when(orderMapper.lockById(1L)).thenReturn(order);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> paymentService.callback(8L, "PAY1", "cb-amt", true, new BigDecimal("9.99")));
        assertEquals(ErrorCode.PARAM_INVALID, ex.getCode());
        verify(inventoryService, never()).confirmSale(anyLong(), anyLong());
    }

    @Test
    void callbackRejectsNonOwnerWithoutLeaking() {
        PaymentAttempt attempt = pendingAttempt(5L, 1L, BigDecimal.TEN);
        PharmacyOrder order = order(1L, 8L, OrderStatus.PENDING_PAYMENT);
        when(paymentMapper.lockByNo("PAY1")).thenReturn(attempt);
        when(orderMapper.lockById(1L)).thenReturn(order);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> paymentService.callback(99L, "PAY1", "cb-own", true, BigDecimal.TEN));
        assertEquals(ErrorCode.NOT_FOUND, ex.getCode());
        assertEquals("支付尝试不存在", ex.getMessage());
    }

    @Test
    void successfulCallbackIsIdempotentOnTerminalStatus() {
        PaymentAttempt attempt = pendingAttempt(5L, 1L, BigDecimal.TEN);
        attempt.setStatus("SUCCESS");
        PharmacyOrder order = order(1L, 8L, OrderStatus.TO_PACK);
        when(paymentMapper.lockByNo("PAY1")).thenReturn(attempt);
        when(orderMapper.lockById(1L)).thenReturn(order);

        paymentService.callback(8L, "PAY1", "cb-dup", true, BigDecimal.TEN);

        verify(inventoryService, never()).confirmSale(anyLong(), anyLong());
        verify(paymentMapper, never()).updateById(any(PaymentAttempt.class));
    }

    @Test
    void successfulCallbackConfirmsSaleOnce() {
        PaymentAttempt attempt = pendingAttempt(5L, 1L, BigDecimal.TEN);
        PharmacyOrder order = order(1L, 8L, OrderStatus.PENDING_PAYMENT);
        when(paymentMapper.lockByNo("PAY1")).thenReturn(attempt);
        when(orderMapper.lockById(1L)).thenReturn(order);

        paymentService.callback(8L, "PAY1", "cb-ok", true, BigDecimal.TEN);

        verify(inventoryService).confirmSale(1L, 8L);
        verify(paymentMapper).updateById(attempt);
        assertEquals("SUCCESS", attempt.getStatus());
        assertEquals(OrderStatus.TO_PACK, order.getOrderStatus());
        verify(events).publishEvent(any(OrderPaidEvent.class));
    }

    @Test
    void illegalOrderStatusRejectedOnCreate() {
        PharmacyOrder order = order(1L, 8L, OrderStatus.TO_PACK);
        when(orderMapper.lockById(1L)).thenReturn(order);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> paymentService.createAttempt(8L, 1L));
        assertEquals(ErrorCode.ORDER_STATUS_CONFLICT, ex.getCode());
    }

    @Test
    void createAttemptReusesPendingAndRejectsExpired() {
        PharmacyOrder payable = order(1L, 8L, OrderStatus.PENDING_PAYMENT);
        when(orderMapper.lockById(1L)).thenReturn(payable);
        PaymentAttempt pending = pendingAttempt(5L, 1L, BigDecimal.TEN);
        when(paymentMapper.selectOne(any())).thenReturn(pending).thenReturn(null);
        assertEquals(5L, paymentService.createAttempt(8L, 1L).getId());
        payable.setPaymentDeadline(LocalDateTime.now().minusMinutes(1));
        assertEquals(ErrorCode.ORDER_STATUS_CONFLICT,
                assertThrows(BusinessException.class, () -> paymentService.createAttempt(8L, 1L)).getCode());
    }

    @Test
    void createAttemptInsertsWhenNonePending() {
        PharmacyOrder payable = order(1L, 8L, OrderStatus.PENDING_PAYMENT);
        when(orderMapper.lockById(1L)).thenReturn(payable);
        when(paymentMapper.selectOne(any())).thenReturn(null);
        paymentService.createAttempt(8L, 1L);
        verify(paymentMapper).insert(any(PaymentAttempt.class));
    }

    @Test
    void failedCallbackMarksAttemptWithoutSale() {
        PaymentAttempt attempt = pendingAttempt(5L, 1L, BigDecimal.TEN);
        PharmacyOrder order = order(1L, 8L, OrderStatus.PENDING_PAYMENT);
        when(paymentMapper.lockByNo("PAY1")).thenReturn(attempt);
        when(orderMapper.lockById(1L)).thenReturn(order);
        paymentService.callback(8L, "PAY1", "cb-fail", false, BigDecimal.TEN);
        assertEquals("FAILED", attempt.getStatus());
        verify(inventoryService, never()).confirmSale(anyLong(), anyLong());
    }

    @Test
    void callbackRejectsBlankKeyAndWrongOrderStatus() {
        assertEquals(ErrorCode.PARAM_INVALID,
                assertThrows(BusinessException.class,
                        () -> paymentService.callback(8L, "PAY1", " ", true, BigDecimal.TEN)).getCode());
        PaymentAttempt attempt = pendingAttempt(5L, 1L, BigDecimal.TEN);
        PharmacyOrder packing = order(1L, 8L, OrderStatus.TO_PACK);
        when(paymentMapper.lockByNo("PAY1")).thenReturn(attempt);
        when(orderMapper.lockById(1L)).thenReturn(packing);
        assertEquals(ErrorCode.ORDER_STATUS_CONFLICT,
                assertThrows(BusinessException.class,
                        () -> paymentService.callback(8L, "PAY1", "cb", true, BigDecimal.TEN)).getCode());
    }

    private static PharmacyOrder order(Long id, Long userId, OrderStatus status) {
        PharmacyOrder order = new PharmacyOrder();
        order.setId(id);
        order.setUserId(userId);
        order.setOrderNo("O" + id);
        order.setOrderAmount(BigDecimal.TEN);
        order.setOrderStatus(status);
        order.setPaymentDeadline(LocalDateTime.now().plusMinutes(20));
        return order;
    }

    private static PaymentAttempt pendingAttempt(Long id, Long orderId, BigDecimal amount) {
        PaymentAttempt attempt = new PaymentAttempt();
        attempt.setId(id);
        attempt.setOrderId(orderId);
        attempt.setPaymentNo("PAY1");
        attempt.setAmount(amount);
        attempt.setStatus("PENDING");
        return attempt;
    }
}
