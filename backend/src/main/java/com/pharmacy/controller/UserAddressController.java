
package com.pharmacy.controller;

import com.pharmacy.common.ApiResponse;
import com.pharmacy.dto.AddressRequest;
import com.pharmacy.service.AddressService;
import com.pharmacy.security.CurrentUser;
import com.pharmacy.vo.AddressVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/user/addresses")
@RequiredArgsConstructor
public class UserAddressController {
    private final AddressService service;
    @GetMapping public ApiResponse<List<AddressVO>> list(){return ApiResponse.success(service.list(CurrentUser.id()));}
    @PostMapping public ApiResponse<AddressVO> add(@Valid @RequestBody AddressRequest r){return ApiResponse.success("新增地址成功",service.add(CurrentUser.id(),r));}
    @PutMapping("/{addressId}") public ApiResponse<AddressVO> update(@PathVariable Long addressId,@Valid @RequestBody AddressRequest r){return ApiResponse.success("地址已更新",service.update(CurrentUser.id(),addressId,r));}
    @DeleteMapping("/{addressId}") public ApiResponse<Void> remove(@PathVariable Long addressId){service.remove(CurrentUser.id(),addressId);return ApiResponse.success("地址已删除",null);}
    @PatchMapping("/{addressId}/default") public ApiResponse<Void> setDefault(@PathVariable Long addressId){service.setDefault(CurrentUser.id(),addressId);return ApiResponse.success("默认地址已更新",null);}
}
