
package com.pharmacy.controller;

import com.pharmacy.common.ApiResponse;
import com.pharmacy.dto.AddressRequest;
import com.pharmacy.service.AddressService;
import com.pharmacy.util.SessionUtil;
import com.pharmacy.vo.AddressVO;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/user/addresses")
@RequiredArgsConstructor
public class UserAddressController {
    private final AddressService service;
    @GetMapping public ApiResponse<List<AddressVO>> list(HttpSession s){return ApiResponse.success(service.list(SessionUtil.require(s).id()));}
    @PostMapping public ApiResponse<AddressVO> add(@Valid @RequestBody AddressRequest r,HttpSession s){return ApiResponse.success("新增地址成功",service.add(SessionUtil.require(s).id(),r));}
    @PutMapping("/{addressId}") public ApiResponse<AddressVO> update(@PathVariable Long addressId,@Valid @RequestBody AddressRequest r,HttpSession s){return ApiResponse.success("地址已更新",service.update(SessionUtil.require(s).id(),addressId,r));}
    @DeleteMapping("/{addressId}") public ApiResponse<Void> remove(@PathVariable Long addressId,HttpSession s){service.remove(SessionUtil.require(s).id(),addressId);return ApiResponse.success("地址已删除",null);}
    @PatchMapping("/{addressId}/default") public ApiResponse<Void> setDefault(@PathVariable Long addressId,HttpSession s){service.setDefault(SessionUtil.require(s).id(),addressId);return ApiResponse.success("默认地址已更新",null);}
}
