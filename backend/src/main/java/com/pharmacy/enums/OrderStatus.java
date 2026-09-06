
package com.pharmacy.enums;

import java.util.EnumSet;

public enum OrderStatus {
    PENDING_ACCEPT("待接单"),
    TO_PACK("待打包"),
    TO_DISPATCH("待派送"),
    DELIVERING("配送中"),
    COMPLETED("已完成"),
    CANCELED("已取消");

    private final String label;
    OrderStatus(String label) { this.label = label; }
    public String getLabel() { return label; }

    public boolean canTransitionTo(OrderStatus target) {
        return switch (this) {
            case PENDING_ACCEPT -> EnumSet.of(TO_PACK, CANCELED).contains(target);
            case TO_PACK -> EnumSet.of(TO_DISPATCH, CANCELED).contains(target);
            case TO_DISPATCH -> EnumSet.of(DELIVERING, CANCELED).contains(target);
            case DELIVERING -> target == COMPLETED;
            default -> false;
        };
    }
}
