package com.pharmacy.audit;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data @TableName("business_audit_log")
public class BusinessAuditLog {
    @TableId(type=IdType.AUTO) private Long id;
    private String traceId; private Long actorId; private String actorRole; private String eventType;
    private String businessType; private String businessId; private String result; private String detailsJson;
    private String clientIp; private LocalDateTime createTime;
}
