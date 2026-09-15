
package com.pharmacy.controller;
import com.pharmacy.common.ApiResponse;
import com.pharmacy.common.PageData;
import com.pharmacy.dto.StatusRequest;
import com.pharmacy.service.AdminUserService;
import com.pharmacy.security.CurrentUser;
import com.pharmacy.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/admin/users") @RequiredArgsConstructor
public class AdminUserController {
 private final AdminUserService service;
 @GetMapping public ApiResponse<PageData<UserVO>> page(@RequestParam(defaultValue="1") long page,@RequestParam(defaultValue="10") long size,@RequestParam(required=false) String keyword,@RequestParam(required=false) String role,@RequestParam(required=false) Integer status){return ApiResponse.success(service.page(page,size,keyword,role,status));}
 @PatchMapping("/{userId}/status") public ApiResponse<Void> status(@PathVariable Long userId,@Valid @RequestBody StatusRequest r){service.updateStatus(CurrentUser.id(),userId,r.getStatus());return ApiResponse.success("用户状态已更新",null);}
 @PatchMapping("/{userId}/role") public ApiResponse<Void> role(@PathVariable Long userId,@Valid @RequestBody RoleRequest r){service.updateRole(CurrentUser.id(),userId,r.role());return ApiResponse.success("用户角色已更新",null);}
 public record RoleRequest(@jakarta.validation.constraints.NotBlank String role){}
}
