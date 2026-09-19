package com.pharmacy.payment;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @TableName("payment_attempt")
public class PaymentAttempt {
    @TableId(type=IdType.AUTO) private Long id;
    private String paymentNo; private Long orderId; private BigDecimal amount; private String status;
    private String callbackKey; private LocalDateTime paidTime; private LocalDateTime createTime; private LocalDateTime updateTime;
}
