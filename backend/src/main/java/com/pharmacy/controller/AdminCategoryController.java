
package com.pharmacy.controller;

import com.pharmacy.common.ApiResponse;
import com.pharmacy.common.PageData;
import com.pharmacy.dto.CategoryRequest;
import com.pharmacy.dto.StatusRequest;
import com.pharmacy.service.CategoryService;
import com.pharmacy.vo.CategoryVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {
    private final CategoryService service;
    @GetMapping public ApiResponse<PageData<CategoryVO>> page(@RequestParam(defaultValue="1") long page,@RequestParam(defaultValue="10") long size,@RequestParam(required=false) String keyword,@RequestParam(required=false) Integer status){return ApiResponse.success(service.page(page,size,keyword,status));}
    @PostMapping public ApiResponse<CategoryVO> create(@Valid @RequestBody CategoryRequest r){return ApiResponse.success("新增分类成功",service.create(r));}
    @PutMapping("/{categoryId}") public ApiResponse<CategoryVO> update(@PathVariable Long categoryId,@Valid @RequestBody CategoryRequest r){return ApiResponse.success("分类已更新",service.update(categoryId,r));}
    @DeleteMapping("/{categoryId}") public ApiResponse<Void> delete(@PathVariable Long categoryId){service.delete(categoryId);return ApiResponse.success("分类已删除",null);}
    @PatchMapping("/{categoryId}/status") public ApiResponse<Void> status(@PathVariable Long categoryId,@Valid @RequestBody StatusRequest r){service.updateStatus(categoryId,r.getStatus());return ApiResponse.success("状态已更新",null);}
}
