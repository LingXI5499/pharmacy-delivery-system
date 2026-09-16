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
import com.pharmacy.messaging.OrderPaidEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentAttemptMapper paymentMapper;
    private final PharmacyOrderMapper orderMapper;
    private final OrderStatusLogMapper logMapper;
    private final InventoryService inventoryService;
    private final ApplicationEventPublisher events;

    @Transactional
    public PaymentAttempt createAttempt(Long userId, Long orderId) {
        PharmacyOrder order = orderMapper.lockById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        if (order.getOrderStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT, "订单当前不可支付");
        }
        if (order.getPaymentDeadline() != null && order.getPaymentDeadline().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT, "订单已超过支付期限");
        }
        PaymentAttempt existing = paymentMapper.selectOne(new LambdaQueryWrapper<PaymentAttempt>()
                .eq(PaymentAttempt::getOrderId, orderId)
                .eq(PaymentAttempt::getStatus, "PENDING")
                .last("LIMIT 1"));
        if (existing != null) return existing;
        PaymentAttempt payment = new PaymentAttempt();
        payment.setPaymentNo("PAY" + System.currentTimeMillis() + orderId);
        payment.setOrderId(orderId);
        payment.setAmount(order.getOrderAmount());
        payment.setStatus("PENDING");
        payment.setCreateTime(LocalDateTime.now());
        payment.setUpdateTime(LocalDateTime.now());
        paymentMapper.insert(payment);
        return payment;
    }

    @Transactional
    public void callback(Long userId, String paymentNo, String callbackKey, boolean success, BigDecimal amount) {
        if (callbackKey == null || callbackKey.isBlank() || callbackKey.length() > 80) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "callbackKey 不合法");
        }
        PaymentAttempt payment = paymentMapper.lockByNo(paymentNo);
        if (payment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "支付尝试不存在");
        }
        PharmacyOrder order = orderMapper.lockById(payment.getOrderId());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "支付尝试不存在");
        }
        if ("SUCCESS".equals(payment.getStatus()) || "FAILED".equals(payment.getStatus())) {
            return;
        }
        if (success) {
            if (amount == null || payment.getAmount().compareTo(amount) != 0) {
                throw new BusinessException(ErrorCode.PARAM_INVALID, "支付金额不一致");
            }
            if (order.getOrderStatus() != OrderStatus.PENDING_PAYMENT) {
                throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT, "订单当前不可确认支付");
            }
            inventoryService.confirmSale(order.getId(), order.getUserId());
            payment.setPaidTime(LocalDateTime.now());
            order.setPaidTime(payment.getPaidTime());
            order.setOrderStatus(OrderStatus.TO_PACK);
            order.setAcceptedTime(payment.getPaidTime());
            order.setUpdateTime(payment.getPaidTime());
            orderMapper.updateById(order);
            addLog(order.getId(), OrderStatus.PENDING_PAYMENT, OrderStatus.TO_PACK, "模拟支付成功");
            events.publishEvent(new OrderPaidEvent(order.getId(), order.getOrderNo(), payment.getPaymentNo(), payment.getPaidTime()));
        }
        payment.setCallbackKey(callbackKey);
        payment.setStatus(success ? "SUCCESS" : "FAILED");
        payment.setUpdateTime(LocalDateTime.now());
        paymentMapper.updateById(payment);
    }

    private void addLog(Long orderId, OrderStatus before, OrderStatus after, String remark) {
        OrderStatusLog log = new OrderStatusLog();
        log.setOrderId(orderId);
        log.setBeforeStatus(before);
        log.setAfterStatus(after);
        log.setOperatorType(OperatorType.SYSTEM);
        log.setRemark(remark);
        log.setCreateTime(LocalDateTime.now());
        logMapper.insert(log);
    }
}
