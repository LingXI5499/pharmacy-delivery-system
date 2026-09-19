
package com.pharmacy.controller;

import com.pharmacy.common.ApiResponse;
import com.pharmacy.common.PageData;
import com.pharmacy.dto.OrderCreateRequest;
import com.pharmacy.dto.ReasonRequest;
import com.pharmacy.service.OrderService;
import com.pharmacy.security.CurrentUser;
import com.pharmacy.vo.OrderDetailVO;
import com.pharmacy.vo.OrderVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/user/orders")
@RequiredArgsConstructor
public class UserOrderController {
    private final OrderService service;
    @PostMapping public ApiResponse<Map<String,Object>> create(@RequestHeader("Idempotency-Key") String idempotencyKey,@Valid @RequestBody OrderCreateRequest r){return ApiResponse.success("订单提交成功",service.create(CurrentUser.id(),r,idempotencyKey));}
    @GetMapping public ApiResponse<PageData<OrderVO>> page(@RequestParam(defaultValue="1") long page,@RequestParam(defaultValue="10") long size,@RequestParam(required=false) String orderStatus){return ApiResponse.success(service.pageUserOrders(CurrentUser.id(),page,size,orderStatus));}
    @GetMapping("/{orderId}") public ApiResponse<OrderDetailVO> detail(@PathVariable Long orderId){return ApiResponse.success(service.userDetail(CurrentUser.id(),orderId));}
    @PostMapping("/{orderId}/cancel") public ApiResponse<Void> cancel(@PathVariable Long orderId,@Valid @RequestBody ReasonRequest r){service.userCancel(CurrentUser.id(),orderId,r);return ApiResponse.success("订单已取消，库存已释放",null);}
}
