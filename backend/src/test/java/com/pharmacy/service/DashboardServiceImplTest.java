package com.pharmacy.service;

import com.pharmacy.entity.Medicine;
import com.pharmacy.entity.MedicineCategory;
import com.pharmacy.entity.PharmacyOrder;
import com.pharmacy.entity.PharmacyOrderItem;
import com.pharmacy.enums.OrderStatus;
import com.pharmacy.mapper.MedicineCategoryMapper;
import com.pharmacy.mapper.MedicineMapper;
import com.pharmacy.mapper.PharmacyOrderItemMapper;
import com.pharmacy.mapper.PharmacyOrderMapper;
import com.pharmacy.service.impl.DashboardServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {
    @Mock private PharmacyOrderMapper orderMapper;
    @Mock private PharmacyOrderItemMapper itemMapper;
    @Mock private MedicineMapper medicineMapper;
    @Mock private MedicineCategoryMapper categoryMapper;
    @InjectMocks private DashboardServiceImpl dashboardService;

    @Test
    void summaryCountsTodayOrdersAndLowStock() {
        PharmacyOrder completed = order(1L, OrderStatus.COMPLETED, new BigDecimal("20.00"));
        PharmacyOrder packing = order(2L, OrderStatus.TO_PACK, new BigDecimal("10.00"));
        when(orderMapper.selectList(any())).thenReturn(List.of(completed, packing));
        when(medicineMapper.selectCount(any())).thenReturn(3L);
        assertEquals(2, dashboardService.summary().todayOrderCount());
        assertEquals(new BigDecimal("20.00"), dashboardService.summary().todaySalesAmount());
        assertEquals(3L, dashboardService.summary().lowStockCount());
    }

    @Test
    void trendsFillMissingDays() {
        when(orderMapper.selectList(any())).thenReturn(List.of());
        assertEquals(7, dashboardService.orderTrend(7).size());
        assertEquals(3, dashboardService.salesTrend(3).size());
    }

    @Test
    void categoryDistributionAndHotMedicinesUseCompletedOrders() {
        PharmacyOrder completed = order(1L, OrderStatus.COMPLETED, new BigDecimal("20.00"));
        when(orderMapper.selectList(any())).thenReturn(List.of(completed));
        PharmacyOrderItem item = new PharmacyOrderItem();
        item.setOrderId(1L);
        item.setMedicineId(9L);
        item.setMedicineName("虚构感冒颗粒");
        item.setMedicineImage("img");
        item.setQuantity(2);
        item.setSubtotalAmount(new BigDecimal("20.00"));
        when(itemMapper.selectList(any())).thenReturn(List.of(item));
        Medicine medicine = new Medicine();
        medicine.setId(9L);
        medicine.setCategoryId(2L);
        when(medicineMapper.selectBatchIds(any())).thenReturn(List.of(medicine));
        MedicineCategory category = new MedicineCategory();
        category.setId(2L);
        category.setCategoryName("感冒用药");
        when(categoryMapper.selectBatchIds(any())).thenReturn(List.of(category));

        assertEquals("感冒用药", dashboardService.categoryDistribution(7).get(0).categoryName());
        assertEquals(2L, dashboardService.hotMedicines(5).get(0).salesQuantity());
    }

    @Test
    void emptyCompletedOrdersReturnEmptyCharts() {
        when(orderMapper.selectList(any())).thenReturn(List.of());
        assertTrue(dashboardService.categoryDistribution(7).isEmpty());
        assertTrue(dashboardService.hotMedicines(5).isEmpty());
    }

    private static PharmacyOrder order(Long id, OrderStatus status, BigDecimal amount) {
        PharmacyOrder order = new PharmacyOrder();
        order.setId(id);
        order.setOrderStatus(status);
        order.setOrderAmount(amount);
        order.setCreateTime(LocalDateTime.now());
        return order;
    }
}
