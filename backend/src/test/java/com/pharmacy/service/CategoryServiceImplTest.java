package com.pharmacy.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pharmacy.common.ErrorCode;
import com.pharmacy.dto.CategoryRequest;
import com.pharmacy.entity.MedicineCategory;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.mapper.MedicineCategoryMapper;
import com.pharmacy.mapper.MedicineMapper;
import com.pharmacy.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {
    @Mock private MedicineCategoryMapper categoryMapper;
    @Mock private MedicineMapper medicineMapper;
    @InjectMocks private CategoryServiceImpl categoryService;

    @Test
    void createRejectsDuplicateName() {
        when(categoryMapper.selectCount(any())).thenReturn(1L);
        BusinessException ex = assertThrows(BusinessException.class, () -> categoryService.create(request("感冒用药")));
        assertEquals(ErrorCode.PARAM_INVALID, ex.getCode());
        verify(categoryMapper, never()).insert(any(MedicineCategory.class));
    }

    @Test
    void createInsertsNewCategory() {
        when(categoryMapper.selectCount(any())).thenReturn(0L);
        when(categoryMapper.insert(any(MedicineCategory.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MedicineCategory.class).setId(2L);
            return 1;
        });
        assertEquals("感冒用药", categoryService.create(request("感冒用药")).categoryName());
    }

    @Test
    void updateRejectsDuplicateNameOnAnotherRow() {
        when(categoryMapper.selectById(2L)).thenReturn(category());
        when(categoryMapper.selectCount(any())).thenReturn(1L);
        BusinessException ex = assertThrows(BusinessException.class, () -> categoryService.update(2L, request("重复")));
        assertEquals(ErrorCode.PARAM_INVALID, ex.getCode());
    }

    @Test
    void deleteRejectsCategoryThatStillHasMedicines() {
        when(categoryMapper.selectById(2L)).thenReturn(category());
        when(medicineMapper.selectCount(any())).thenReturn(3L);
        BusinessException ex = assertThrows(BusinessException.class, () -> categoryService.delete(2L));
        assertEquals(ErrorCode.CATEGORY_NOT_EMPTY, ex.getCode());
        verify(categoryMapper, never()).deleteById(2L);
    }

    @Test
    void deleteAndStatusUpdateRequireExistingRow() {
        when(categoryMapper.selectById(2L)).thenReturn(category());
        when(medicineMapper.selectCount(any())).thenReturn(0L);
        categoryService.delete(2L);
        verify(categoryMapper).deleteById(2L);

        categoryService.updateStatus(2L, 0);
        verify(categoryMapper).updateById(any(MedicineCategory.class));
    }

    @Test
    void pageDelegatesToMapper() {
        when(categoryMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<MedicineCategory> page = invocation.getArgument(0);
            page.setRecords(List.of(category()));
            page.setTotal(1);
            return page;
        });
        assertEquals(1, categoryService.page(1, 10, "感", 1).total());
    }

    @Test
    void missingCategoryIsNotFound() {
        when(categoryMapper.selectById(2L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> categoryService.updateStatus(2L, 1));
        assertEquals(ErrorCode.NOT_FOUND, ex.getCode());
    }

    private static CategoryRequest request(String name) {
        CategoryRequest request = new CategoryRequest();
        request.setCategoryName(name);
        request.setSortNo(1);
        request.setStatus(1);
        request.setDescription(" ");
        return request;
    }

    private static MedicineCategory category() {
        MedicineCategory category = new MedicineCategory();
        category.setId(2L);
        category.setCategoryName("感冒用药");
        category.setSortNo(1);
        category.setStatus(1);
        return category;
    }
}
