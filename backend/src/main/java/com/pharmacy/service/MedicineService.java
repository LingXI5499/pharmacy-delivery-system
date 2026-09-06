package com.pharmacy.service;
import com.pharmacy.common.PageData;
import com.pharmacy.dto.MedicineRequest;
import com.pharmacy.dto.StockAdjustRequest;
import com.pharmacy.vo.MedicineVO;
public interface MedicineService { PageData<MedicineVO> page(long page,long size,String keyword,Long categoryId,Integer status,String stockStatus); MedicineVO create(MedicineRequest request); MedicineVO update(Long id,MedicineRequest request); void delete(Long id); void adjustStock(Long id,StockAdjustRequest request); void updateStatus(Long id,Integer status); PageData<MedicineVO> lowStock(long page,long size); }
