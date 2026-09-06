
package com.pharmacy.vo;
import java.math.BigDecimal;
public record DashboardSummaryVO(long todayOrderCount, BigDecimal todaySalesAmount, long pendingOrderCount, long lowStockCount, long toAcceptCount, long toPackCount, long toDispatchCount, long deliveringCount, long completedCount) { }
