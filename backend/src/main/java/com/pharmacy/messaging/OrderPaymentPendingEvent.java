package com.pharmacy.messaging;
import java.time.LocalDateTime;
public record OrderPaymentPendingEvent(Long orderId,String orderNo,LocalDateTime deadline) {}
