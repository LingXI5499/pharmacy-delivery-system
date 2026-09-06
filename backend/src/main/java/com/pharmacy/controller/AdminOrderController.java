
package com.pharmacy.controller;

import com.pharmacy.common.ApiResponse;
import com.pharmacy.common.PageData;
import com.pharmacy.dto.DispatchRequest;
import com.pharmacy.dto.ReasonRequest;
import com.pharmacy.dto.RemarkRequest;
import com.pharmacy.service.OrderService;
import com.pharmacy.util.SessionUtil;
import com.pharmacy.vo.OrderDetailVO;
import com.pharmacy.vo.OrderVO;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {
    private final OrderService service;
    @GetMapping public ApiResponse<PageData<OrderVO>> page(@RequestParam(defaultValue="1") long page,@RequestParam(defaultValue="10") long size,@RequestParam(required=false) String orderNo,@RequestParam(required=false) String orderStatus,@RequestParam(required=false) String keyword,@RequestParam(required=false) Long riderId,@RequestParam(required=false) @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,@RequestParam(required=false) @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss") LocalDateTime endTime){return ApiResponse.success(service.pageAdminOrders(page,size,orderNo,orderStatus,keyword,riderId,startTime,endTime));}
    @GetMapping("/{orderId}") public ApiResponse<OrderDetailVO> detail(@PathVariable Long orderId){return ApiResponse.success(service.adminDetail(orderId));}
    @PostMapping("/{orderId}/accept") public ApiResponse<Void> accept(@PathVariable Long orderId,@Valid @RequestBody RemarkRequest r,HttpSession s){service.accept(SessionUtil.currentAdmin(s).id(),orderId,r);return ApiResponse.success("已接单，等待打包",null);}
    @PostMapping("/{orderId}/pack") public ApiResponse<Void> pack(@PathVariable Long orderId,@Valid @RequestBody RemarkRequest r,HttpSession s){service.pack(SessionUtil.currentAdmin(s).id(),orderId,r);return ApiResponse.success("已完成打包，等待派送",null);}
    @PostMapping("/{orderId}/dispatch") public ApiResponse<Void> dispatch(@PathVariable Long orderId,@Valid @RequestBody DispatchRequest r,HttpSession s){service.dispatch(SessionUtil.currentAdmin(s).id(),orderId,r);return ApiResponse.success("已派单，订单配送中",null);}
    @PostMapping("/{orderId}/complete") public ApiResponse<Void> complete(@PathVariable Long orderId,@Valid @RequestBody RemarkRequest r,HttpSession s){service.complete(SessionUtil.currentAdmin(s).id(),orderId,r);return ApiResponse.success("订单已完成",null);}
    @PostMapping("/{orderId}/cancel") public ApiResponse<Void> cancel(@PathVariable Long orderId,@Valid @RequestBody ReasonRequest r,HttpSession s){service.adminCancel(SessionUtil.currentAdmin(s).id(),orderId,r);return ApiResponse.success("订单已取消，库存已恢复",null);}
}
