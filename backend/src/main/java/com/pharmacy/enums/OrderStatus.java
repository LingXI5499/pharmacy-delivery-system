
package com.pharmacy.enums;

import java.util.EnumSet;

public enum OrderStatus {
    PENDING_REVIEW("待处方审核"),
    PENDING_PAYMENT("待支付"),
    // Read compatibility only. V2 never creates this state.
    PENDING_ACCEPT("旧版待接单"),
    TO_PACK("待打包"),
    TO_DISPATCH("待派送"),
    DELIVERING("配送中"),
    COMPLETED("已完成"),
    CANCELED("已取消"),
    REVIEW_REJECTED("处方已驳回"),
    CLOSED_TIMEOUT("超时关闭"),
    CLOSED_STOCK_SHORTAGE("库存不足关闭"),
    REFUNDING("退款中"),
    REFUNDED("已退款");

    private final String label;
    OrderStatus(String label) { this.label = label; }
    public String getLabel() { return label; }

    public boolean canTransitionTo(OrderStatus target) {
        return switch (this) {
            case PENDING_REVIEW -> EnumSet.of(PENDING_PAYMENT, REVIEW_REJECTED, CLOSED_STOCK_SHORTAGE, CANCELED).contains(target);
            case PENDING_PAYMENT -> EnumSet.of(TO_PACK, CANCELED, CLOSED_TIMEOUT).contains(target);
            case PENDING_ACCEPT -> EnumSet.of(TO_PACK, CANCELED).contains(target);
            case TO_PACK -> EnumSet.of(TO_DISPATCH, CANCELED, REFUNDING).contains(target);
            case TO_DISPATCH -> EnumSet.of(DELIVERING, CANCELED, REFUNDING).contains(target);
            case DELIVERING -> EnumSet.of(COMPLETED, REFUNDING).contains(target);
            case COMPLETED -> target == REFUNDING;
            case REFUNDING -> target == REFUNDED;
            default -> false;
        };
    }
}
