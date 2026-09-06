
package com.pharmacy.controller;

import com.pharmacy.common.ApiResponse;
import com.pharmacy.common.PageData;
import com.pharmacy.dto.OrderCreateRequest;
import com.pharmacy.dto.ReasonRequest;
import com.pharmacy.service.OrderService;
import com.pharmacy.util.SessionUtil;
import com.pharmacy.vo.OrderDetailVO;
import com.pharmacy.vo.OrderVO;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/user/orders")
@RequiredArgsConstructor
public class UserOrderController {
    private final OrderService service;
    @PostMapping public ApiResponse<Map<String,Object>> create(@Valid @RequestBody OrderCreateRequest r,HttpSession s){return ApiResponse.success("订单提交成功",service.create(SessionUtil.require(s).id(),r));}
    @GetMapping public ApiResponse<PageData<OrderVO>> page(@RequestParam(defaultValue="1") long page,@RequestParam(defaultValue="10") long size,@RequestParam(required=false) String orderStatus,HttpSession s){return ApiResponse.success(service.pageUserOrders(SessionUtil.require(s).id(),page,size,orderStatus));}
    @GetMapping("/{orderId}") public ApiResponse<OrderDetailVO> detail(@PathVariable Long orderId,HttpSession s){return ApiResponse.success(service.userDetail(SessionUtil.require(s).id(),orderId));}
    @PostMapping("/{orderId}/cancel") public ApiResponse<Void> cancel(@PathVariable Long orderId,@Valid @RequestBody ReasonRequest r,HttpSession s){service.userCancel(SessionUtil.require(s).id(),orderId,r);return ApiResponse.success("订单已取消，库存已恢复",null);}
}
