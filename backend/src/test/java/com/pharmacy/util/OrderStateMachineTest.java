
package com.pharmacy.util;

import com.pharmacy.enums.OrderStatus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OrderStateMachineTest {
    @Test
    void shouldAllowOnlyDocumentedTransitions() {
        assertTrue(OrderStateMachine.canTransition(OrderStatus.PENDING_ACCEPT, OrderStatus.TO_PACK));
        assertTrue(OrderStateMachine.canTransition(OrderStatus.TO_PACK, OrderStatus.CANCELED));
        assertTrue(OrderStateMachine.canTransition(OrderStatus.DELIVERING, OrderStatus.COMPLETED));
        assertFalse(OrderStateMachine.canTransition(OrderStatus.PENDING_ACCEPT, OrderStatus.COMPLETED));
        assertFalse(OrderStateMachine.canTransition(OrderStatus.COMPLETED, OrderStatus.DELIVERING));
        assertFalse(OrderStateMachine.canTransition(OrderStatus.DELIVERING, OrderStatus.CANCELED));
        assertTrue(OrderStateMachine.canTransition(OrderStatus.PENDING_REVIEW, OrderStatus.PENDING_PAYMENT));
        assertTrue(OrderStateMachine.canTransition(OrderStatus.PENDING_REVIEW, OrderStatus.REVIEW_REJECTED));
        assertTrue(OrderStateMachine.canTransition(OrderStatus.PENDING_PAYMENT, OrderStatus.TO_PACK));
        assertTrue(OrderStateMachine.canTransition(OrderStatus.PENDING_PAYMENT, OrderStatus.CLOSED_TIMEOUT));
        assertTrue(OrderStateMachine.canTransition(OrderStatus.TO_DISPATCH, OrderStatus.DELIVERING));
        assertTrue(OrderStateMachine.canTransition(OrderStatus.COMPLETED, OrderStatus.REFUNDING));
        assertTrue(OrderStateMachine.canTransition(OrderStatus.REFUNDING, OrderStatus.REFUNDED));
        assertFalse(OrderStateMachine.canTransition(OrderStatus.REFUNDED, OrderStatus.COMPLETED));
        assertFalse(OrderStateMachine.canTransition(null, OrderStatus.TO_PACK));
        assertEquals("待支付", OrderStatus.PENDING_PAYMENT.getLabel());
    }
}
