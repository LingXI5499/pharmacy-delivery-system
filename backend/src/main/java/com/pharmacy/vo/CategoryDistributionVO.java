
package com.pharmacy.vo;
import java.math.BigDecimal;
public record CategoryDistributionVO(String categoryName, BigDecimal amount, Long quantity) { }
