
package com.pharmacy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pharmacy.common.ErrorCode;
import com.pharmacy.common.PageData;
import com.pharmacy.entity.Medicine;
import com.pharmacy.entity.MedicineCategory;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.mapper.MedicineCategoryMapper;
import com.pharmacy.mapper.MedicineMapper;
import com.pharmacy.service.PublicMedicineService;
import com.pharmacy.vo.CategoryVO;
import com.pharmacy.vo.MedicineVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublicMedicineServiceImpl implements PublicMedicineService {
    private final MedicineMapper medicineMapper;
    private final MedicineCategoryMapper categoryMapper;

    @Override
    public List<CategoryVO> listEnabledCategories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<MedicineCategory>()
                        .eq(MedicineCategory::getStatus, 1)
                        .orderByAsc(MedicineCategory::getSortNo).orderByDesc(MedicineCategory::getId))
                .stream().map(PublicMedicineServiceImpl::toCategoryVO).toList();
    }

    @Override
    public PageData<MedicineVO> pageMedicines(long page, long size, String keyword, Long categoryId, String sort) {
        Page<Medicine> pageObj = new Page<>(Math.max(page, 1), Math.min(Math.max(size, 1), 50));
        LambdaQueryWrapper<Medicine> q = new LambdaQueryWrapper<Medicine>()
                .eq(Medicine::getStatus, 1)
                .eq(categoryId != null, Medicine::getCategoryId, categoryId)
                .like(keyword != null && !keyword.isBlank(), Medicine::getMedicineName, keyword);
        if ("priceAsc".equals(sort)) q.orderByAsc(Medicine::getPrice);
        else if ("priceDesc".equals(sort)) q.orderByDesc(Medicine::getPrice);
        else if ("stockDesc".equals(sort)) q.orderByDesc(Medicine::getStock);
        else q.orderByDesc(Medicine::getCreateTime);
        medicineMapper.selectPage(pageObj, q);
        Map<Long, MedicineCategory> categories = categoryMap(pageObj.getRecords());
        return PageData.from(pageObj, m -> toMedicineVO(m, categories.get(m.getCategoryId())));
    }

    @Override
    public MedicineVO getMedicine(Long medicineId) {
        Medicine medicine = medicineMapper.selectById(medicineId);
        if (medicine == null || Integer.valueOf(0).equals(medicine.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "药品不存在或已下架");
        }
        MedicineCategory category = categoryMapper.selectById(medicine.getCategoryId());
        return toMedicineVO(medicine, category);
    }

    private Map<Long, MedicineCategory> categoryMap(List<Medicine> medicines) {
        List<Long> ids = medicines.stream().map(Medicine::getCategoryId).distinct().toList();
        if (ids.isEmpty()) return Map.of();
        return categoryMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(MedicineCategory::getId, Function.identity()));
    }

    static MedicineVO toMedicineVO(Medicine m, MedicineCategory c) {
        boolean low = m.getStock() != null && m.getWarningStock() != null && m.getStock() <= m.getWarningStock();
        return new MedicineVO(m.getId(), m.getCategoryId(), c == null ? "未分类" : c.getCategoryName(), m.getMedicineName(), m.getImageUrl(), m.getDescription(), m.getUsageInstruction(), m.getPrecautions(), m.getPrice(), m.getStock(), m.getWarningStock(), m.getStatus(), low, m.getCreateTime(), m.getUpdateTime());
    }
    static CategoryVO toCategoryVO(MedicineCategory c) { return new CategoryVO(c.getId(), c.getCategoryName(), c.getCategoryImage(), c.getDescription(), c.getSortNo(), c.getStatus(), c.getCreateTime(), c.getUpdateTime()); }
}
