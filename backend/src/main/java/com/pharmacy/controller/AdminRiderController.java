
package com.pharmacy.controller;
import com.pharmacy.common.ApiResponse;
import com.pharmacy.common.PageData;
import com.pharmacy.dto.RiderRequest;
import com.pharmacy.dto.StatusRequest;
import com.pharmacy.service.RiderService;
import com.pharmacy.vo.RiderVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/admin/riders") @RequiredArgsConstructor
public class AdminRiderController {
 private final RiderService service;
 @GetMapping public ApiResponse<PageData<RiderVO>> page(@RequestParam(defaultValue="1") long page,@RequestParam(defaultValue="10") long size,@RequestParam(required=false) String keyword,@RequestParam(required=false) Integer status){return ApiResponse.success(service.page(page,size,keyword,status));}
 @GetMapping("/available") public ApiResponse<List<RiderVO>> available(){return ApiResponse.success(service.available());}
 @PostMapping public ApiResponse<RiderVO> create(@Valid @RequestBody RiderRequest r){return ApiResponse.success("新增骑手成功",service.create(r));}
 @PutMapping("/{riderId}") public ApiResponse<RiderVO> update(@PathVariable Long riderId,@Valid @RequestBody RiderRequest r){return ApiResponse.success("骑手已更新",service.update(riderId,r));}
 @PatchMapping("/{riderId}/status") public ApiResponse<Void> status(@PathVariable Long riderId,@Valid @RequestBody StatusRequest r){service.updateStatus(riderId,r.getStatus());return ApiResponse.success("状态已更新",null);}
 @DeleteMapping("/{riderId}") public ApiResponse<Void> delete(@PathVariable Long riderId){service.delete(riderId);return ApiResponse.success("骑手已删除",null);}
}
