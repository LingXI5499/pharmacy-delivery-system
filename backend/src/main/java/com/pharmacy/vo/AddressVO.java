
package com.pharmacy.vo;
import java.time.LocalDateTime;
public record AddressVO(Long id, String receiverName, String receiverPhone, String province, String city, String district, String detailAddress, Integer isDefault, LocalDateTime createTime, LocalDateTime updateTime) { }
