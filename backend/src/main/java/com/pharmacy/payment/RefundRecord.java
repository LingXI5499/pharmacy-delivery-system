package com.pharmacy.payment;
import com.baomidou.mybatisplus.annotation.*;import lombok.Data;import java.math.BigDecimal;import java.time.LocalDateTime;
@Data @TableName("refund_record") public class RefundRecord {@TableId(type=IdType.AUTO)private Long id;private String refundNo;private Long paymentId;private Long orderId;private BigDecimal amount;private String status;private String callbackKey;private String reason;private String originalOrderStatus;private Integer restockRequired;private LocalDateTime refundedTime;private LocalDateTime createTime;private LocalDateTime updateTime;}
