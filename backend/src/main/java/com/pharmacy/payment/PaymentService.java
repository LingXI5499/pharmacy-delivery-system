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
import java.time.LocalDateTime;

@Service @RequiredArgsConstructor
public class PaymentService {
    private final PaymentAttemptMapper paymentMapper; private final PharmacyOrderMapper orderMapper;
    private final OrderStatusLogMapper logMapper; private final InventoryService inventoryService;
    private final ApplicationEventPublisher events;

    @Transactional public PaymentAttempt createAttempt(Long userId,Long orderId){
        PharmacyOrder order=orderMapper.lockById(orderId);if(order==null||!order.getUserId().equals(userId))throw new BusinessException(ErrorCode.NOT_FOUND,"订单不存在");
        if(order.getOrderStatus()!=OrderStatus.PENDING_PAYMENT)throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT,"订单当前不可支付");
        if(order.getPaymentDeadline()!=null&&order.getPaymentDeadline().isBefore(LocalDateTime.now()))throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT,"订单已超过支付期限");
        PaymentAttempt existing=paymentMapper.selectOne(new LambdaQueryWrapper<PaymentAttempt>().eq(PaymentAttempt::getOrderId,orderId).eq(PaymentAttempt::getStatus,"PENDING").last("LIMIT 1"));if(existing!=null)return existing;
        PaymentAttempt p=new PaymentAttempt();p.setPaymentNo("PAY"+System.currentTimeMillis()+orderId);p.setOrderId(orderId);p.setAmount(order.getOrderAmount());p.setStatus("PENDING");p.setCreateTime(LocalDateTime.now());p.setUpdateTime(LocalDateTime.now());paymentMapper.insert(p);return p;
    }

    @Transactional public void callback(Long userId,String paymentNo,String callbackKey,boolean success){
        if(callbackKey==null||callbackKey.isBlank()||callbackKey.length()>80)throw new BusinessException(ErrorCode.PARAM_INVALID,"callbackKey 不合法");
        PaymentAttempt p=paymentMapper.lockByNo(paymentNo);if(p==null)throw new BusinessException(ErrorCode.NOT_FOUND,"支付尝试不存在");
        PharmacyOrder order=orderMapper.lockById(p.getOrderId());
        if(!order.getUserId().equals(userId))throw new BusinessException(ErrorCode.NOT_FOUND,"支付尝试不存在");
        if("SUCCESS".equals(p.getStatus())||"FAILED".equals(p.getStatus()))return;
        p.setCallbackKey(callbackKey);p.setStatus(success?"SUCCESS":"FAILED");p.setUpdateTime(LocalDateTime.now());
        if(success){if(order.getOrderStatus()!=OrderStatus.PENDING_PAYMENT)throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT,"订单当前不可确认支付");inventoryService.confirmSale(order.getId(),order.getUserId());p.setPaidTime(LocalDateTime.now());order.setPaidTime(p.getPaidTime());order.setOrderStatus(OrderStatus.TO_PACK);order.setAcceptedTime(p.getPaidTime());order.setUpdateTime(p.getPaidTime());orderMapper.updateById(order);addLog(order.getId(),OrderStatus.PENDING_PAYMENT,OrderStatus.TO_PACK,"模拟支付成功");events.publishEvent(new OrderPaidEvent(order.getId(),order.getOrderNo(),p.getPaymentNo(),p.getPaidTime()));}
        paymentMapper.updateById(p);
    }
    private void addLog(Long orderId,OrderStatus before,OrderStatus after,String remark){OrderStatusLog l=new OrderStatusLog();l.setOrderId(orderId);l.setBeforeStatus(before);l.setAfterStatus(after);l.setOperatorType(OperatorType.SYSTEM);l.setRemark(remark);l.setCreateTime(LocalDateTime.now());logMapper.insert(l);}
}
