
package com.pharmacy.controller;

import com.pharmacy.common.ApiResponse;
import com.pharmacy.service.DashboardService;
import com.pharmacy.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {
    private final DashboardService service;
    @GetMapping("/summary") public ApiResponse<DashboardSummaryVO> summary(){return ApiResponse.success(service.summary());}
    @GetMapping("/order-trend") public ApiResponse<List<TrendPointVO>> orderTrend(@RequestParam(defaultValue="7") int days){return ApiResponse.success(service.orderTrend(days));}
    @GetMapping("/sales-trend") public ApiResponse<List<TrendPointVO>> salesTrend(@RequestParam(defaultValue="7") int days){return ApiResponse.success(service.salesTrend(days));}
    @GetMapping("/category-distribution") public ApiResponse<List<CategoryDistributionVO>> category(@RequestParam(defaultValue="7") int days){return ApiResponse.success(service.categoryDistribution(days));}
    @GetMapping("/hot-medicines") public ApiResponse<List<HotMedicineVO>> hot(@RequestParam(defaultValue="5") int limit){return ApiResponse.success(service.hotMedicines(limit));}
}
