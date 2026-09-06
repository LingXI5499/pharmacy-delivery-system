package com.pharmacy.service;
import com.pharmacy.common.PageData;
import com.pharmacy.vo.CategoryVO;
import com.pharmacy.vo.MedicineVO;
import java.util.List;
public interface PublicMedicineService { List<CategoryVO> listEnabledCategories(); PageData<MedicineVO> pageMedicines(long page,long size,String keyword,Long categoryId,String sort); MedicineVO getMedicine(Long medicineId); }
