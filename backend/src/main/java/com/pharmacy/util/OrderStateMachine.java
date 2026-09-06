
package com.pharmacy.util;

import com.pharmacy.enums.OrderStatus;

public final class OrderStateMachine {
    private OrderStateMachine() { }
    public static boolean canTransition(OrderStatus current, OrderStatus target) {
        return current != null && target != null && current.canTransitionTo(target);
    }
}
