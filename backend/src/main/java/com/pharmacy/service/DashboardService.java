package com.pharmacy.service;
import com.pharmacy.vo.*;
import java.util.List;
public interface DashboardService { DashboardSummaryVO summary(); List<TrendPointVO> orderTrend(int days); List<TrendPointVO> salesTrend(int days); List<CategoryDistributionVO> categoryDistribution(int days); List<HotMedicineVO> hotMedicines(int limit); }
