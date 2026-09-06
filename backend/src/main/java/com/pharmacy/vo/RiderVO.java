
package com.pharmacy.vo;
import java.time.LocalDateTime;
public record RiderVO(Long id, String riderName, String phone, Integer status, String remark, LocalDateTime createTime, LocalDateTime updateTime) { }
