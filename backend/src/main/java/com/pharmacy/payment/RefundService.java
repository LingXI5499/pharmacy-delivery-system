package com.pharmacy.payment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pharmacy.common.ErrorCode;
import com.pharmacy.entity.OrderStatusLog;
import com.pharmacy.entity.PharmacyOrder;
import com.pharmacy.enums.OperatorType;
import com.pharmacy.enums.OrderStatus;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.inventory.InventoryService;
import com.pharmacy.mapper.OrderStatusLogMapper;
import com.pharmacy.mapper.PharmacyOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RefundService {
    private static final Set<OrderStatus> REFUNDABLE = Set.of(
            OrderStatus.TO_PACK, OrderStatus.TO_DISPATCH, OrderStatus.DELIVERING, OrderStatus.COMPLETED);
    private static final Set<OrderStatus> RESTOCKABLE = Set.of(OrderStatus.TO_PACK, OrderStatus.TO_DISPATCH);

    private final RefundRecordMapper refunds;
    private final PaymentAttemptMapper payments;
    private final PharmacyOrderMapper orders;
    private final OrderStatusLogMapper logs;
    private final InventoryService inventory;

    @Transactional
    public RefundRecord request(Long userId, Long orderId, String reason) {
        return requestInternal(userId, orderId, reason, false);
    }

    @Transactional
    public RefundRecord requestByAdmin(Long adminId, Long orderId, String reason) {
        return requestInternal(adminId, orderId, reason, true);
    }

    private RefundRecord requestInternal(Long actorId, Long orderId, String reason, boolean privileged) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "退款原因不能为空");
        }
        PharmacyOrder order = orders.lockById(orderId);
        if (order == null || (!privileged && !order.getUserId().equals(actorId))) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        RefundRecord existing = refunds.selectOne(new LambdaQueryWrapper<RefundRecord>()
                .eq(RefundRecord::getOrderId, orderId));
        if (existing != null) return existing;
        if (!REFUNDABLE.contains(order.getOrderStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT, "订单当前不可退款");
        }
        PaymentAttempt payment = payments.selectOne(new LambdaQueryWrapper<PaymentAttempt>()
                .eq(PaymentAttempt::getOrderId, orderId).eq(PaymentAttempt::getStatus, "SUCCESS").last("LIMIT 1"));
        if (payment == null) throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT, "未找到成功支付记录");

        RefundRecord refund = new RefundRecord();
        refund.setRefundNo("REF" + System.currentTimeMillis() + orderId);
        refund.setPaymentId(payment.getId());
        refund.setOrderId(orderId);
        refund.setAmount(payment.getAmount());
        refund.setStatus("PENDING");
        refund.setReason(reason);
        refund.setOriginalOrderStatus(order.getOrderStatus().name());
        refund.setRestockRequired(RESTOCKABLE.contains(order.getOrderStatus()) ? 1 : 0);
        refund.setCreateTime(LocalDateTime.now());
        refund.setUpdateTime(LocalDateTime.now());
        refunds.insert(refund);

        OrderStatus before = order.getOrderStatus();
        order.setOrderStatus(OrderStatus.REFUNDING);
        order.setUpdateTime(LocalDateTime.now());
        orders.updateById(order);
        log(orderId, before, OrderStatus.REFUNDING, (privileged ? "管理员发起退款：" : "用户申请退款：") + reason);
        return refund;
    }

    @Transactional
    public void callback(String refundNo, String callbackKey, boolean success) {
        if (callbackKey == null || callbackKey.isBlank() || callbackKey.length() > 80) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "callbackKey 不合法");
        }
        RefundRecord refund = refunds.lockByNo(refundNo);
        if (refund == null) throw new BusinessException(ErrorCode.NOT_FOUND, "退款记录不存在");
        if (!"PENDING".equals(refund.getStatus())) return;
        PharmacyOrder order = orders.lockById(refund.getOrderId());
        refund.setCallbackKey(callbackKey);
        refund.setStatus(success ? "SUCCESS" : "FAILED");
        refund.setUpdateTime(LocalDateTime.now());
        if (success) {
            if (Integer.valueOf(1).equals(refund.getRestockRequired())) {
                inventory.refundRestock(order.getId(), order.getUserId());
            }
            refund.setRefundedTime(LocalDateTime.now());
            order.setOrderStatus(OrderStatus.REFUNDED);
            order.setUpdateTime(LocalDateTime.now());
            orders.updateById(order);
            log(order.getId(), OrderStatus.REFUNDING, OrderStatus.REFUNDED,
                    Integer.valueOf(1).equals(refund.getRestockRequired())
                            ? "退款成功并回补未发货库存" : "退款成功，已配送商品不自动回补库存");
        } else {
            OrderStatus original = OrderStatus.valueOf(refund.getOriginalOrderStatus());
            order.setOrderStatus(original);
            order.setUpdateTime(LocalDateTime.now());
            orders.updateById(order);
            log(order.getId(), OrderStatus.REFUNDING, original, "退款失败，订单恢复原状态");
        }
        refunds.updateById(refund);
    }

    private void log(Long orderId, OrderStatus before, OrderStatus after, String remark) {
        OrderStatusLog row = new OrderStatusLog();
        row.setOrderId(orderId);
        row.setBeforeStatus(before);
        row.setAfterStatus(after);
        row.setOperatorType(OperatorType.SYSTEM);
        row.setRemark(remark);
        row.setCreateTime(LocalDateTime.now());
        logs.insert(row);
    }
}
