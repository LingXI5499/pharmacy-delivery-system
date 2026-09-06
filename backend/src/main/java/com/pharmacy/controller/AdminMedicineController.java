
package com.pharmacy.controller;

import com.pharmacy.common.ApiResponse;
import com.pharmacy.common.PageData;
import com.pharmacy.dto.MedicineRequest;
import com.pharmacy.dto.StatusRequest;
import com.pharmacy.dto.StockAdjustRequest;
import com.pharmacy.service.MedicineService;
import com.pharmacy.vo.MedicineVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/medicines")
@RequiredArgsConstructor
public class AdminMedicineController {
    private final MedicineService service;
    @GetMapping public ApiResponse<PageData<MedicineVO>> page(@RequestParam(defaultValue="1") long page,@RequestParam(defaultValue="10") long size,@RequestParam(required=false) String keyword,@RequestParam(required=false) Long categoryId,@RequestParam(required=false) Integer status,@RequestParam(required=false) String stockStatus){return ApiResponse.success(service.page(page,size,keyword,categoryId,status,stockStatus));}
    @PostMapping public ApiResponse<MedicineVO> create(@Valid @RequestBody MedicineRequest r){return ApiResponse.success("新增药品成功",service.create(r));}
    @PutMapping("/{medicineId}") public ApiResponse<MedicineVO> update(@PathVariable Long medicineId,@Valid @RequestBody MedicineRequest r){return ApiResponse.success("药品已更新",service.update(medicineId,r));}
    @DeleteMapping("/{medicineId}") public ApiResponse<Void> delete(@PathVariable Long medicineId){service.delete(medicineId);return ApiResponse.success("药品已删除",null);}
    @PatchMapping("/{medicineId}/stock") public ApiResponse<Void> stock(@PathVariable Long medicineId,@Valid @RequestBody StockAdjustRequest r){service.adjustStock(medicineId,r);return ApiResponse.success("库存已调整",null);}
    @PatchMapping("/{medicineId}/status") public ApiResponse<Void> status(@PathVariable Long medicineId,@Valid @RequestBody StatusRequest r){service.updateStatus(medicineId,r.getStatus());return ApiResponse.success("状态已更新",null);}
    @GetMapping("/low-stock") public ApiResponse<PageData<MedicineVO>> low(@RequestParam(defaultValue="1") long page,@RequestParam(defaultValue="10") long size){return ApiResponse.success(service.lowStock(page,size));}
}
