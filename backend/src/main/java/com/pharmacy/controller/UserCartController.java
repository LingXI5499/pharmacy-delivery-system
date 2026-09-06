
package com.pharmacy.controller;

import com.pharmacy.common.ApiResponse;
import com.pharmacy.dto.CartAddRequest;
import com.pharmacy.dto.CartQuantityRequest;
import com.pharmacy.dto.CartSelectedRequest;
import com.pharmacy.service.CartService;
import com.pharmacy.util.SessionUtil;
import com.pharmacy.vo.CartVO;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/cart")
@RequiredArgsConstructor
public class UserCartController {
    private final CartService service;
    @GetMapping public ApiResponse<CartVO> get(HttpSession s){return ApiResponse.success(service.getCart(SessionUtil.require(s).id()));}
    @PostMapping("/items") public ApiResponse<Void> add(@Valid @RequestBody CartAddRequest r,HttpSession s){service.add(SessionUtil.require(s).id(),r);return ApiResponse.success("加入购物车成功",null);}
    @PutMapping("/items/{cartItemId}") public ApiResponse<Void> quantity(@PathVariable Long cartItemId,@Valid @RequestBody CartQuantityRequest r,HttpSession s){service.updateQuantity(SessionUtil.require(s).id(),cartItemId,r.getQuantity());return ApiResponse.success("购物车已更新",null);}
    @PatchMapping("/items/{cartItemId}/selected") public ApiResponse<Void> selected(@PathVariable Long cartItemId,@Valid @RequestBody CartSelectedRequest r,HttpSession s){service.updateSelected(SessionUtil.require(s).id(),cartItemId,r.getSelected());return ApiResponse.success(null);}
    @DeleteMapping("/items/{cartItemId}") public ApiResponse<Void> remove(@PathVariable Long cartItemId,HttpSession s){service.remove(SessionUtil.require(s).id(),cartItemId);return ApiResponse.success("已删除",null);}
    @DeleteMapping("/invalid-items") public ApiResponse<Integer> clearInvalid(HttpSession s){return ApiResponse.success("已清除失效商品",service.clearInvalid(SessionUtil.require(s).id()));}
}
