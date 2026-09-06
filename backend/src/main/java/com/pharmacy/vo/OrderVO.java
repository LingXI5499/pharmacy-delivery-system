
package com.pharmacy.vo;
import com.pharmacy.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public record OrderVO(Long id, String orderNo, String username, Long userId, String receiverName, String receiverPhone, String receiverAddress, BigDecimal productAmount, BigDecimal deliveryFee, BigDecimal orderAmount, OrderStatus orderStatus, String orderStatusName, Long riderId, String riderName, String riderPhone, String userRemark, String adminRemark, String cancelReason, LocalDateTime createTime) { }
