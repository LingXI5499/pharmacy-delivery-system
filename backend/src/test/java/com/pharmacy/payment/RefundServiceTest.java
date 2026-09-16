package com.pharmacy.payment;

import com.pharmacy.common.ErrorCode;
import com.pharmacy.entity.PharmacyOrder;
import com.pharmacy.enums.OrderStatus;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.inventory.InventoryService;
import com.pharmacy.mapper.OrderStatusLogMapper;
import com.pharmacy.mapper.PharmacyOrderMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {
    @Mock private RefundRecordMapper refunds;
    @Mock private PaymentAttemptMapper payments;
    @Mock private PharmacyOrderMapper orders;
    @Mock private OrderStatusLogMapper logs;
    @Mock private InventoryService inventory;
    @InjectMocks private RefundService refundService;

    @Test
    void callbackRestocksOnlyWhenRequired() {
        RefundRecord refund = pendingRefund(1L, 10L, 1, new BigDecimal("10.00"));
        PharmacyOrder order = order(10L, 8L, OrderStatus.REFUNDING);
        when(refunds.lockByNo("REF1")).thenReturn(refund);
        when(orders.lockById(10L)).thenReturn(order);

        refundService.callback("REF1", "rcb-1", true, new BigDecimal("10.00"));

        verify(inventory).refundRestock(10L, 8L);
        assertEquals("SUCCESS", refund.getStatus());
        assertEquals(OrderStatus.REFUNDED, order.getOrderStatus());
    }

    @Test
    void callbackSkipsRestockForDeliveredOrders() {
        RefundRecord refund = pendingRefund(2L, 11L, 0, new BigDecimal("10.00"));
        PharmacyOrder order = order(11L, 8L, OrderStatus.REFUNDING);
        when(refunds.lockByNo("REF2")).thenReturn(refund);
        when(orders.lockById(11L)).thenReturn(order);

        refundService.callback("REF2", "rcb-2", true, new BigDecimal("10.00"));

        verify(inventory, never()).refundRestock(anyLong(), anyLong());
        assertEquals(OrderStatus.REFUNDED, order.getOrderStatus());
    }

    @Test
    void callbackIsIdempotentWhenAlreadyTerminal() {
        RefundRecord refund = pendingRefund(3L, 12L, 1, BigDecimal.TEN);
        refund.setStatus("SUCCESS");
        when(refunds.lockByNo("REF3")).thenReturn(refund);

        refundService.callback("REF3", "rcb-3", true, BigDecimal.TEN);

        verify(inventory, never()).refundRestock(anyLong(), anyLong());
        verify(orders, never()).updateById(any(PharmacyOrder.class));
    }

    @Test
    void callbackRejectsAmountMismatch() {
        RefundRecord refund = pendingRefund(4L, 13L, 1, new BigDecimal("10.00"));
        when(refunds.lockByNo("REF4")).thenReturn(refund);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> refundService.callback("REF4", "rcb-4", true, new BigDecimal("1.00")));
        assertEquals(ErrorCode.PARAM_INVALID, ex.getCode());
        verify(inventory, never()).refundRestock(anyLong(), anyLong());
    }

    @Test
    void callbackRejectsMissingRefund() {
        when(refunds.lockByNo("gone")).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> refundService.callback("gone", "rcb", true, BigDecimal.TEN));
        assertEquals(ErrorCode.NOT_FOUND, ex.getCode());
    }

    @Test
    void requestRejectsNonOwner() {
        PharmacyOrder order = order(20L, 8L, OrderStatus.TO_PACK);
        when(orders.lockById(20L)).thenReturn(order);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> refundService.request(99L, 20L, "不想要了"));
        assertEquals(ErrorCode.NOT_FOUND, ex.getCode());
    }

    private static RefundRecord pendingRefund(Long id, Long orderId, int restockRequired, BigDecimal amount) {
        RefundRecord refund = new RefundRecord();
        refund.setId(id);
        refund.setRefundNo("REF" + id);
        refund.setOrderId(orderId);
        refund.setAmount(amount);
        refund.setStatus("PENDING");
        refund.setRestockRequired(restockRequired);
        refund.setOriginalOrderStatus(OrderStatus.TO_PACK.name());
        return refund;
    }

    private static PharmacyOrder order(Long id, Long userId, OrderStatus status) {
        PharmacyOrder order = new PharmacyOrder();
        order.setId(id);
        order.setUserId(userId);
        order.setOrderStatus(status);
        return order;
    }
}
