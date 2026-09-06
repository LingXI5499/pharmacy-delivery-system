
package com.pharmacy.vo;
import com.pharmacy.enums.OperatorType;
import com.pharmacy.enums.OrderStatus;
import java.time.LocalDateTime;
public record OrderStatusLogVO(Long id, OrderStatus beforeStatus, String beforeStatusName, OrderStatus afterStatus, String afterStatusName, OperatorType operatorType, Long operatorId, String remark, LocalDateTime createTime) { }
