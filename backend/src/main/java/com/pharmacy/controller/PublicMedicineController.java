
package com.pharmacy.controller;

import com.pharmacy.common.ApiResponse;
import com.pharmacy.common.PageData;
import com.pharmacy.service.PublicMedicineService;
import com.pharmacy.vo.CategoryVO;
import com.pharmacy.vo.MedicineVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicMedicineController {
    private final PublicMedicineService service;
    @GetMapping("/categories") public ApiResponse<List<CategoryVO>> categories(){return ApiResponse.success(service.listEnabledCategories());}
    @GetMapping("/medicines") public ApiResponse<PageData<MedicineVO>> medicines(@RequestParam(defaultValue="1") long page,@RequestParam(defaultValue="12") long size,@RequestParam(required=false) String keyword,@RequestParam(required=false) Long categoryId,@RequestParam(defaultValue="default") String sort){return ApiResponse.success(service.pageMedicines(page,size,keyword,categoryId,sort));}
    @GetMapping("/medicines/{medicineId}") public ApiResponse<MedicineVO> detail(@PathVariable Long medicineId){return ApiResponse.success(service.getMedicine(medicineId));}
}
