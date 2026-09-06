
package com.pharmacy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pharmacy.enums.OperatorType;
import com.pharmacy.enums.OrderStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("order_status_log")
public class OrderStatusLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private OrderStatus beforeStatus;
    private OrderStatus afterStatus;
    private OperatorType operatorType;
    private Long operatorId;
    private String remark;
    private LocalDateTime createTime;
}
