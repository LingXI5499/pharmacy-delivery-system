package com.pharmacy.procurement;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data @TableName("supplier")
public class Supplier {
    @TableId(type=IdType.AUTO) private Long id;
    private String supplierCode; private String supplierName; private String contactName; private String phone;
    private Integer status; private LocalDateTime createTime; private LocalDateTime updateTime;
}
