
package com.pharmacy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("shopping_cart")
public class ShoppingCart {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long medicineId;
    private Integer quantity;
    private Integer selected;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
