
package com.pharmacy.vo;
import com.pharmacy.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
public record OrderDetailVO(Long id, String orderNo, String username, Long userId, String receiverName, String receiverPhone, String receiverAddress, BigDecimal productAmount, BigDecimal deliveryFee, BigDecimal orderAmount, OrderStatus orderStatus, String orderStatusName, Long riderId, String riderName, String riderPhone, String userRemark, String adminRemark, String cancelReason, LocalDateTime acceptedTime, LocalDateTime packedTime, LocalDateTime dispatchedTime, LocalDateTime completedTime, LocalDateTime canceledTime, LocalDateTime createTime, List<OrderItemVO> items, List<OrderStatusLogVO> statusLogs) { }
