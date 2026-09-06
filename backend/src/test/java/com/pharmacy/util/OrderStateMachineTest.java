
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
    }
}
