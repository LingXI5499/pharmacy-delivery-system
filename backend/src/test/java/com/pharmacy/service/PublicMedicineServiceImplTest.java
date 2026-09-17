package com.pharmacy.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pharmacy.common.ErrorCode;
import com.pharmacy.entity.Medicine;
import com.pharmacy.entity.MedicineCategory;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.mapper.MedicineCategoryMapper;
import com.pharmacy.mapper.MedicineMapper;
import com.pharmacy.service.impl.PublicMedicineServiceImpl;
import com.pharmacy.vo.MedicineVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicMedicineServiceImplTest {
    @Mock private MedicineMapper medicineMapper;
    @Mock private MedicineCategoryMapper categoryMapper;
    @InjectMocks private PublicMedicineServiceImpl publicMedicineService;

    @Test
    void listEnabledCategories() {
        MedicineCategory category = new MedicineCategory();
        category.setId(1L);
        category.setCategoryName("感冒用药");
        when(categoryMapper.selectList(any())).thenReturn(List.of(category));
        assertEquals("感冒用药", publicMedicineService.listEnabledCategories().get(0).categoryName());
    }

    @Test
    void pageMedicinesSupportsSortKeysAndEmptyPage() {
        when(medicineMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<Medicine> page = invocation.getArgument(0);
            page.setRecords(List.of());
            page.setTotal(0);
            return page;
        });
        assertEquals(0, publicMedicineService.pageMedicines(1, 12, "感", 1L, "priceAsc").total());
        assertEquals(0, publicMedicineService.pageMedicines(1, 12, null, null, "priceDesc").total());
        assertEquals(0, publicMedicineService.pageMedicines(1, 12, null, null, "stockDesc").total());
        assertEquals(0, publicMedicineService.pageMedicines(1, 12, null, null, "default").total());
    }

    @Test
    void getMedicineHidesOffShelfItems() {
        Medicine medicine = new Medicine();
        medicine.setId(9L);
        medicine.setStatus(0);
        when(medicineMapper.selectById(9L)).thenReturn(medicine);
        BusinessException ex = assertThrows(BusinessException.class, () -> publicMedicineService.getMedicine(9L));
        assertEquals(ErrorCode.NOT_FOUND, ex.getCode());
    }

    @Test
    void getMedicineMarksLowStock() {
        Medicine medicine = new Medicine();
        medicine.setId(9L);
        medicine.setStatus(1);
        medicine.setCategoryId(2L);
        medicine.setMedicineName("虚构感冒颗粒");
        medicine.setPrice(new BigDecimal("10.00"));
        medicine.setStock(2);
        medicine.setWarningStock(5);
        when(medicineMapper.selectById(9L)).thenReturn(medicine);
        when(categoryMapper.selectById(2L)).thenReturn(null);
        MedicineVO vo = publicMedicineService.getMedicine(9L);
        assertTrue(vo.isLowStock());
        assertEquals("未分类", vo.categoryName());
    }
}
