
package com.pharmacy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pharmacy.common.ErrorCode;
import com.pharmacy.common.PageData;
import com.pharmacy.dto.MedicineRequest;
import com.pharmacy.dto.StockAdjustRequest;
import com.pharmacy.entity.Medicine;
import com.pharmacy.entity.MedicineCategory;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.mapper.MedicineCategoryMapper;
import com.pharmacy.mapper.MedicineMapper;
import com.pharmacy.service.MedicineService;
import com.pharmacy.vo.MedicineVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MedicineServiceImpl implements MedicineService {
    private final MedicineMapper medicineMapper;
    private final MedicineCategoryMapper categoryMapper;

    @Override
    public PageData<MedicineVO> page(long page,long size,String keyword,Long categoryId,Integer status,String stockStatus) {
        Page<Medicine> p=new Page<>(Math.max(page,1),Math.min(Math.max(size,1),100));
        LambdaQueryWrapper<Medicine> q=new LambdaQueryWrapper<Medicine>().like(keyword!=null&&!keyword.isBlank(),Medicine::getMedicineName,keyword)
                .eq(categoryId!=null,Medicine::getCategoryId,categoryId).eq(status!=null,Medicine::getStatus,status);
        if ("LOW".equalsIgnoreCase(stockStatus)) q.gt(Medicine::getStock,0).apply("stock <= warning_stock");
        else if ("OUT".equalsIgnoreCase(stockStatus)) q.eq(Medicine::getStock,0);
        else if ("NORMAL".equalsIgnoreCase(stockStatus)) q.apply("stock > warning_stock");
        q.orderByDesc(Medicine::getUpdateTime);
        medicineMapper.selectPage(p,q);
        Map<Long,MedicineCategory> categories=categoryMap(p.getRecords());
        return PageData.from(p,m->PublicMedicineServiceImpl.toMedicineVO(m,categories.get(m.getCategoryId())));
    }
    @Override
    public MedicineVO create(MedicineRequest r) { ensureCategory(r.getCategoryId()); Medicine m=new Medicine(); copy(r,m); m.setVersion(0);m.setIsDeleted(0);m.setCreateTime(LocalDateTime.now());m.setUpdateTime(LocalDateTime.now());medicineMapper.insert(m);return toVO(m); }
    @Override
    public MedicineVO update(Long id,MedicineRequest r) { ensureCategory(r.getCategoryId()); Medicine m=get(id);copy(r,m);m.setUpdateTime(LocalDateTime.now());medicineMapper.updateById(m);return toVO(m); }
    @Override
    public void delete(Long id) { get(id); medicineMapper.deleteById(id); }
    @Override
    @Transactional(rollbackFor=Exception.class)
    public void adjustStock(Long id,StockAdjustRequest r) {
        Medicine m=get(id); int old=m.getStock(); int result;
        String type=r.getAdjustType()==null?"":r.getAdjustType().trim().toUpperCase();
        switch(type){case "INCREMENT" -> result=old+r.getQuantity();case "DECREMENT" -> result=old-r.getQuantity();case "SET" -> result=r.getQuantity();default -> throw new BusinessException(ErrorCode.PARAM_INVALID,"adjustType 仅支持 INCREMENT、DECREMENT、SET");}
        if(result<0) throw new BusinessException(ErrorCode.PARAM_INVALID,"调整后库存不能小于 0");
        m.setStock(result);m.setVersion((m.getVersion()==null?0:m.getVersion())+1);m.setUpdateTime(LocalDateTime.now());medicineMapper.updateById(m);
    }
    @Override
    public void updateStatus(Long id,Integer status){Medicine m=get(id);m.setStatus(status);m.setUpdateTime(LocalDateTime.now());medicineMapper.updateById(m);}
    @Override
    public PageData<MedicineVO> lowStock(long page,long size){return page(page,size,null,null,null,"LOW");}
    private Medicine get(Long id){Medicine m=medicineMapper.selectById(id);if(m==null)throw new BusinessException(ErrorCode.NOT_FOUND,"药品不存在");return m;}
    private void ensureCategory(Long id){MedicineCategory c=categoryMapper.selectById(id);if(c==null)throw new BusinessException(ErrorCode.NOT_FOUND,"药品分类不存在");}
    private void copy(MedicineRequest r,Medicine m){m.setCategoryId(r.getCategoryId());m.setMedicineName(r.getMedicineName());m.setImageUrl(blank(r.getImageUrl()));m.setDescription(blank(r.getDescription()));m.setUsageInstruction(blank(r.getUsageInstruction()));m.setPrecautions(blank(r.getPrecautions()));m.setPrice(r.getPrice());m.setStock(r.getStock());m.setWarningStock(r.getWarningStock());m.setStatus(r.getStatus());}
    private MedicineVO toVO(Medicine m){return PublicMedicineServiceImpl.toMedicineVO(m,categoryMapper.selectById(m.getCategoryId()));}
    private Map<Long,MedicineCategory> categoryMap(List<Medicine> medicines){List<Long> ids=medicines.stream().map(Medicine::getCategoryId).distinct().toList();if(ids.isEmpty())return Map.of();return categoryMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(MedicineCategory::getId, Function.identity()));}
    private static String blank(String s){return s==null||s.isBlank()?null:s;}
}
