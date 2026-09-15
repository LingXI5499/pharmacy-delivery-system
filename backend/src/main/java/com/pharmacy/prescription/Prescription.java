package com.pharmacy.prescription;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data @TableName("prescription")
public class Prescription {
    @TableId(type=IdType.AUTO) private Long id;
    private String prescriptionNo; private Long userId; private Long orderId; private String storageKey;
    private String originalFilename; private String contentType; private Long sizeBytes; private String sha256;
    private String status; private Long reviewerId; private String reviewReason; private LocalDateTime reviewedTime;
    private LocalDateTime createTime; private LocalDateTime updateTime;
}
