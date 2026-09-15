
package com.pharmacy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("medicine")
public class Medicine {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long categoryId;
    private String medicineName;
    private String imageUrl;
    private String description;
    private String usageInstruction;
    private String precautions;
    private BigDecimal price;
    private Integer stock;
    private Integer warningStock;
    private Integer prescriptionRequired;
    private Integer status;
    @Version
    private Integer version;
    @TableLogic
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
