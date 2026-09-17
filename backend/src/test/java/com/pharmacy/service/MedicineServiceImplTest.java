package com.pharmacy.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pharmacy.common.ErrorCode;
import com.pharmacy.dto.MedicineRequest;
import com.pharmacy.dto.StockAdjustRequest;
import com.pharmacy.entity.Medicine;
import com.pharmacy.entity.MedicineCategory;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.inventory.InventoryService;
import com.pharmacy.mapper.MedicineCategoryMapper;
import com.pharmacy.mapper.MedicineMapper;
import com.pharmacy.service.impl.MedicineServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicineServiceImplTest {
    @Mock private MedicineMapper medicineMapper;
    @Mock private MedicineCategoryMapper categoryMapper;
    @Mock private InventoryService inventoryService;
    @InjectMocks private MedicineServiceImpl medicineService;

    @Test
    void createForcesAggregateStockToZero() {
        when(categoryMapper.selectById(2L)).thenReturn(category());
        when(medicineMapper.insert(any(Medicine.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, Medicine.class).setId(9L);
            return 1;
        });
        medicineService.create(request());
        ArgumentCaptor<Medicine> captor = ArgumentCaptor.forClass(Medicine.class);
        verify(medicineMapper).insert(captor.capture());
        assertEquals(0, captor.getValue().getStock());
    }

    @Test
    void createRejectsMissingCategory() {
        when(categoryMapper.selectById(2L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> medicineService.create(request()));
        assertEquals(ErrorCode.NOT_FOUND, ex.getCode());
    }

    @Test
    void updateAndDeleteRequireExistingMedicine() {
        when(categoryMapper.selectById(2L)).thenReturn(category());
        when(medicineMapper.selectById(9L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> medicineService.update(9L, request()));
        assertEquals(ErrorCode.NOT_FOUND, ex.getCode());
        BusinessException missing = assertThrows(BusinessException.class, () -> medicineService.delete(9L));
        assertEquals(ErrorCode.NOT_FOUND, missing.getCode());
    }

    @Test
    void adjustStockDelegatesToBatchInventory() {
        when(medicineMapper.selectById(9L)).thenReturn(medicine());
        StockAdjustRequest request = new StockAdjustRequest();
        request.setBatchId(11L);
        request.setAdjustType("INCREMENT");
        request.setQuantity(2);
        request.setRemark("验收");
        medicineService.adjustStock(9L, request);
        verify(inventoryService).adjustBatch(11L, "INCREMENT", 2, "验收", null);
    }

    @Test
    void pageSupportsStockFiltersAndStatusChange() {
        when(medicineMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<Medicine> page = invocation.getArgument(0);
            page.setRecords(List.of(medicine()));
            page.setTotal(1);
            return page;
        });
        when(categoryMapper.selectBatchIds(any())).thenReturn(List.of(category()));
        assertEquals(1, medicineService.page(1, 10, "感冒", 2L, 1, "LOW").total());
        assertEquals(1, medicineService.page(1, 10, null, null, null, "OUT").total());
        assertEquals(1, medicineService.lowStock(1, 10).total());

        when(medicineMapper.selectById(9L)).thenReturn(medicine());
        medicineService.updateStatus(9L, 0);
        verify(medicineMapper).updateById(any(Medicine.class));
    }

    private static MedicineRequest request() {
        MedicineRequest request = new MedicineRequest();
        request.setCategoryId(2L);
        request.setMedicineName("虚构感冒颗粒");
        request.setPrice(new BigDecimal("12.00"));
        request.setStock(99);
        request.setWarningStock(5);
        request.setPrescriptionRequired(0);
        request.setStatus(1);
        request.setImageUrl(" ");
        return request;
    }

    private static Medicine medicine() {
        Medicine medicine = new Medicine();
        medicine.setId(9L);
        medicine.setCategoryId(2L);
        medicine.setMedicineName("虚构感冒颗粒");
        medicine.setPrice(new BigDecimal("12.00"));
        medicine.setStock(3);
        medicine.setWarningStock(5);
        medicine.setStatus(1);
        return medicine;
    }

    private static MedicineCategory category() {
        MedicineCategory category = new MedicineCategory();
        category.setId(2L);
        category.setCategoryName("感冒用药");
        return category;
    }
}
