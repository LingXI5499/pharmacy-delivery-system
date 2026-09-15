package com.pharmacy.service;
import com.pharmacy.common.PageData;
import com.pharmacy.dto.DispatchRequest;
import com.pharmacy.dto.OrderCreateRequest;
import com.pharmacy.dto.ReasonRequest;
import com.pharmacy.dto.RemarkRequest;
import com.pharmacy.vo.OrderDetailVO;
import com.pharmacy.vo.OrderVO;
import java.time.LocalDateTime;
import java.util.Map;
public interface OrderService {
 Map<String,Object> create(Long userId, OrderCreateRequest request, String idempotencyKey); PageData<OrderVO> pageUserOrders(Long userId,long page,long size,String orderStatus); OrderDetailVO userDetail(Long userId,Long orderId); void userCancel(Long userId,Long orderId,ReasonRequest request);
 PageData<OrderVO> pageAdminOrders(long page,long size,String orderNo,String orderStatus,String keyword,Long riderId,LocalDateTime startTime,LocalDateTime endTime); OrderDetailVO adminDetail(Long orderId); void accept(Long adminId,Long orderId,RemarkRequest request); void pack(Long adminId,Long orderId,RemarkRequest request); void dispatch(Long adminId,Long orderId,DispatchRequest request); void complete(Long adminId,Long orderId,RemarkRequest request); void adminCancel(Long adminId,Long orderId,ReasonRequest request);
}
