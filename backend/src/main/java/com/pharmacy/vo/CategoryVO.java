
package com.pharmacy.vo;
import java.time.LocalDateTime;
public record CategoryVO(Long id, String categoryName, String categoryImage, String description, Integer sortNo, Integer status, LocalDateTime createTime, LocalDateTime updateTime) { }
