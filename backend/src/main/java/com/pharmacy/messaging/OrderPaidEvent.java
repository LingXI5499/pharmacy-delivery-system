package com.pharmacy.messaging;
import java.time.LocalDateTime;
public record OrderPaidEvent(Long orderId,String orderNo,String paymentNo,LocalDateTime paidAt) {}
