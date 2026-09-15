package com.pharmacy.messaging;

import com.pharmacy.entity.OrderStatusLog;
import com.pharmacy.entity.PharmacyOrder;
import com.pharmacy.enums.OperatorType;
import com.pharmacy.enums.OrderStatus;
import com.pharmacy.inventory.InventoryService;
import com.pharmacy.mapper.OrderStatusLogMapper;
import com.pharmacy.mapper.PharmacyOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service @RequiredArgsConstructor
public class OrderTimeoutService {
    private final PharmacyOrderMapper orderMapper; private final OrderStatusLogMapper logMapper; private final InventoryService inventory;
    @Transactional public void closeIfPending(Long orderId){PharmacyOrder order=orderMapper.lockById(orderId);if(order==null||order.getOrderStatus()!=OrderStatus.PENDING_PAYMENT)return;if(order.getPaymentDeadline()!=null&&order.getPaymentDeadline().isAfter(LocalDateTime.now()))return;inventory.release(orderId,"支付超时释放库存",null);order.setOrderStatus(OrderStatus.CLOSED_TIMEOUT);order.setCanceledTime(LocalDateTime.now());order.setCancelReason("超过 30 分钟未支付");order.setUpdateTime(LocalDateTime.now());orderMapper.updateById(order);OrderStatusLog l=new OrderStatusLog();l.setOrderId(orderId);l.setBeforeStatus(OrderStatus.PENDING_PAYMENT);l.setAfterStatus(OrderStatus.CLOSED_TIMEOUT);l.setOperatorType(OperatorType.SYSTEM);l.setRemark("支付超时自动关单");l.setCreateTime(LocalDateTime.now());logMapper.insert(l);}
}
