
package com.pharmacy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pharmacy.common.ErrorCode;
import com.pharmacy.common.PageData;
import com.pharmacy.dto.CategoryRequest;
import com.pharmacy.entity.Medicine;
import com.pharmacy.entity.MedicineCategory;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.mapper.MedicineCategoryMapper;
import com.pharmacy.mapper.MedicineMapper;
import com.pharmacy.service.CategoryService;
import com.pharmacy.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final MedicineCategoryMapper categoryMapper;
    private final MedicineMapper medicineMapper;

    @Override
    public PageData<CategoryVO> page(long page, long size, String keyword, Integer status) {
        Page<MedicineCategory> p = new Page<>(Math.max(page,1), Math.min(Math.max(size,1),100));
        categoryMapper.selectPage(p, new LambdaQueryWrapper<MedicineCategory>().like(keyword != null && !keyword.isBlank(), MedicineCategory::getCategoryName,keyword)
                .eq(status != null,MedicineCategory::getStatus,status).orderByAsc(MedicineCategory::getSortNo).orderByDesc(MedicineCategory::getId));
        return PageData.from(p, PublicMedicineServiceImpl::toCategoryVO);
    }

    @Override
    public CategoryVO create(CategoryRequest request) {
        long exists = categoryMapper.selectCount(new LambdaQueryWrapper<MedicineCategory>().eq(MedicineCategory::getCategoryName, request.getCategoryName()));
        if (exists > 0) throw new BusinessException(ErrorCode.PARAM_INVALID, "分类名称已存在");
        MedicineCategory c = new MedicineCategory(); copy(request,c); c.setIsDeleted(0); c.setCreateTime(LocalDateTime.now()); c.setUpdateTime(LocalDateTime.now()); categoryMapper.insert(c);
        return PublicMedicineServiceImpl.toCategoryVO(c);
    }

    @Override
    public CategoryVO update(Long id, CategoryRequest request) {
        MedicineCategory c = get(id);
        long exists = categoryMapper.selectCount(new LambdaQueryWrapper<MedicineCategory>().eq(MedicineCategory::getCategoryName, request.getCategoryName()).ne(MedicineCategory::getId,id));
        if (exists > 0) throw new BusinessException(ErrorCode.PARAM_INVALID, "分类名称已存在");
        copy(request,c); c.setUpdateTime(LocalDateTime.now()); categoryMapper.updateById(c); return PublicMedicineServiceImpl.toCategoryVO(c);
    }

    @Override
    public void delete(Long id) {
        get(id);
        long count = medicineMapper.selectCount(new LambdaQueryWrapper<Medicine>().eq(Medicine::getCategoryId,id));
        if (count > 0) throw new BusinessException(ErrorCode.CATEGORY_NOT_EMPTY,"分类下存在药品，不允许删除");
        categoryMapper.deleteById(id);
    }

    @Override
    public void updateStatus(Long id, Integer status) { MedicineCategory c=get(id); c.setStatus(status); c.setUpdateTime(LocalDateTime.now()); categoryMapper.updateById(c); }
    private MedicineCategory get(Long id) { MedicineCategory c=categoryMapper.selectById(id); if(c==null) throw new BusinessException(ErrorCode.NOT_FOUND,"分类不存在"); return c; }
    private static void copy(CategoryRequest r, MedicineCategory c) { c.setCategoryName(r.getCategoryName()); c.setCategoryImage(blank(r.getCategoryImage())); c.setDescription(blank(r.getDescription())); c.setSortNo(r.getSortNo()); c.setStatus(r.getStatus()); }
    private static String blank(String s) { return s==null||s.isBlank()?null:s; }
}
