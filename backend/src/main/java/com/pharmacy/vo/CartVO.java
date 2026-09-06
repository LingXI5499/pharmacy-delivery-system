
package com.pharmacy.vo;
import java.math.BigDecimal;
import java.util.List;
public record CartVO(List<CartItemVO> items, Integer selectedCount, BigDecimal selectedAmount) { }
