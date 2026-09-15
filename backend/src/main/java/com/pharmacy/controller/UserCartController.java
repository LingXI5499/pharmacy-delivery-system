
package com.pharmacy.controller;

import com.pharmacy.common.ApiResponse;
import com.pharmacy.dto.CartAddRequest;
import com.pharmacy.dto.CartQuantityRequest;
import com.pharmacy.dto.CartSelectedRequest;
import com.pharmacy.service.CartService;
import com.pharmacy.security.CurrentUser;
import com.pharmacy.vo.CartVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/cart")
@RequiredArgsConstructor
public class UserCartController {
    private final CartService service;
    @GetMapping public ApiResponse<CartVO> get(){return ApiResponse.success(service.getCart(CurrentUser.id()));}
    @PostMapping("/items") public ApiResponse<Void> add(@Valid @RequestBody CartAddRequest r){service.add(CurrentUser.id(),r);return ApiResponse.success("加入购物车成功",null);}
    @PutMapping("/items/{cartItemId}") public ApiResponse<Void> quantity(@PathVariable Long cartItemId,@Valid @RequestBody CartQuantityRequest r){service.updateQuantity(CurrentUser.id(),cartItemId,r.getQuantity());return ApiResponse.success("购物车已更新",null);}
    @PatchMapping("/items/{cartItemId}/selected") public ApiResponse<Void> selected(@PathVariable Long cartItemId,@Valid @RequestBody CartSelectedRequest r){service.updateSelected(CurrentUser.id(),cartItemId,r.getSelected());return ApiResponse.success(null);}
    @DeleteMapping("/items/{cartItemId}") public ApiResponse<Void> remove(@PathVariable Long cartItemId){service.remove(CurrentUser.id(),cartItemId);return ApiResponse.success("已删除",null);}
    @DeleteMapping("/invalid-items") public ApiResponse<Integer> clearInvalid(){return ApiResponse.success("已清除失效商品",service.clearInvalid(CurrentUser.id()));}
}
