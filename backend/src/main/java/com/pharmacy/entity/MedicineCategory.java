
package com.pharmacy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("medicine_category")
public class MedicineCategory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String categoryName;
    private String categoryImage;
    private String description;
    private Integer sortNo;
    private Integer status;
    @TableLogic
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
