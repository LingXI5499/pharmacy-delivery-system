
package com.pharmacy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pharmacy.common.ErrorCode;
import com.pharmacy.common.PageData;
import com.pharmacy.dto.*;
import com.pharmacy.entity.*;
import com.pharmacy.enums.OperatorType;
import com.pharmacy.enums.OrderStatus;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.mapper.*;
import com.pharmacy.service.OrderService;
import com.pharmacy.util.OrderNoUtil;
import com.pharmacy.util.OrderStateMachine;
import com.pharmacy.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final PharmacyOrderMapper orderMapper;
    private final PharmacyOrderItemMapper itemMapper;
    private final OrderStatusLogMapper logMapper;
    private final UserAddressMapper addressMapper;
    private final ShoppingCartMapper cartMapper;
    private final MedicineMapper medicineMapper;
    private final DeliveryRiderMapper riderMapper;
    private final SysUserMapper userMapper;
    @Value("${app.order.delivery-fee:5.00}")
    private BigDecimal deliveryFee;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> create(Long userId, OrderCreateRequest request) {
        UserAddress address = addressMapper.selectById(request.getAddressId());
        if (address == null || !Objects.equals(address.getUserId(), userId)) {
            throw new BusinessException(ErrorCode.ADDRESS_NOT_OWNED, "当前地址不属于登录用户");
        }
        List<Long> cartIds = request.getCartItemIds().stream().distinct().toList();
        List<ShoppingCart> carts = cartMapper.selectList(new LambdaQueryWrapper<ShoppingCart>()
                .eq(ShoppingCart::getUserId, userId).in(ShoppingCart::getId, cartIds));
        if (carts.size() != cartIds.size()) throw new BusinessException(ErrorCode.PARAM_INVALID, "购物车商品不存在或不属于当前用户");
        List<Medicine> medicines = medicineMapper.selectBatchIds(carts.stream().map(ShoppingCart::getMedicineId).toList());
        Map<Long, Medicine> medicineMap = medicines.stream().collect(Collectors.toMap(Medicine::getId, Function.identity()));
        BigDecimal productAmount = BigDecimal.ZERO;
        for (ShoppingCart cart : carts) {
            Medicine m = medicineMap.get(cart.getMedicineId());
            if (m == null || !Integer.valueOf(1).equals(m.getStatus())) throw new BusinessException(ErrorCode.STOCK_OR_STATUS_CONFLICT, "药品已下架或不存在");
            int affected = medicineMapper.decreaseStock(m.getId(), cart.getQuantity());
            if (affected != 1) throw new BusinessException(ErrorCode.STOCK_OR_STATUS_CONFLICT, "药品库存不足：" + m.getMedicineName());
            productAmount = productAmount.add(m.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity())));
        }
        PharmacyOrder order = new PharmacyOrder();
        order.setOrderNo(OrderNoUtil.next());
        order.setUserId(userId); order.setAddressId(address.getId());
        order.setReceiverName(address.getReceiverName()); order.setReceiverPhone(address.getReceiverPhone()); order.setReceiverAddress(fullAddress(address));
        order.setProductAmount(productAmount); order.setDeliveryFee(deliveryFee); order.setOrderAmount(productAmount.add(deliveryFee));
        order.setOrderStatus(OrderStatus.PENDING_ACCEPT); order.setUserRemark(blank(request.getUserRemark()));
        order.setCreateTime(LocalDateTime.now()); order.setUpdateTime(LocalDateTime.now());
        orderMapper.insert(order);
        for (ShoppingCart cart : carts) {
            Medicine m = medicineMap.get(cart.getMedicineId());
            PharmacyOrderItem item = new PharmacyOrderItem();
            item.setOrderId(order.getId()); item.setMedicineId(m.getId()); item.setMedicineName(m.getMedicineName()); item.setMedicineImage(m.getImageUrl()); item.setMedicinePrice(m.getPrice()); item.setQuantity(cart.getQuantity()); item.setSubtotalAmount(m.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity()))); item.setCreateTime(LocalDateTime.now());
            itemMapper.insert(item);
        }
        addLog(order.getId(), null, OrderStatus.PENDING_ACCEPT, OperatorType.SYSTEM, null, "用户提交订单");
        cartMapper.deleteByIds(cartIds);
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("orderId", order.getId()); result.put("orderNo", order.getOrderNo()); result.put("orderStatus", order.getOrderStatus()); result.put("orderAmount", order.getOrderAmount());
        return result;
    }

    @Override
    public PageData<OrderVO> pageUserOrders(Long userId, long page, long size, String orderStatus) {
        Page<PharmacyOrder> p = new Page<>(Math.max(page,1), Math.min(Math.max(size,1),100));
        LambdaQueryWrapper<PharmacyOrder> q = new LambdaQueryWrapper<PharmacyOrder>().eq(PharmacyOrder::getUserId,userId).orderByDesc(PharmacyOrder::getCreateTime);
        addStatusCondition(q, orderStatus);
        orderMapper.selectPage(p,q);
        return PageData.from(p, o -> toOrderVO(o, null));
    }

    @Override
    public OrderDetailVO userDetail(Long userId, Long orderId) {
        PharmacyOrder order = get(orderId);
        if (!Objects.equals(order.getUserId(), userId)) throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看该订单");
        return toDetail(order, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void userCancel(Long userId, Long orderId, ReasonRequest request) {
        PharmacyOrder order = get(orderId);
        if (!Objects.equals(order.getUserId(), userId)) throw new BusinessException(ErrorCode.FORBIDDEN, "无权取消该订单");
        cancelInternal(order, EnumSet.of(OrderStatus.PENDING_ACCEPT), request.getReason(), OperatorType.USER, userId);
    }

    @Override
    public PageData<OrderVO> pageAdminOrders(long page, long size, String orderNo, String orderStatus, String keyword, Long riderId, LocalDateTime startTime, LocalDateTime endTime) {
        Page<PharmacyOrder> p = new Page<>(Math.max(page,1),Math.min(Math.max(size,1),100));
        LambdaQueryWrapper<PharmacyOrder> q = new LambdaQueryWrapper<PharmacyOrder>().like(orderNo!=null&&!orderNo.isBlank(),PharmacyOrder::getOrderNo,orderNo)
                .eq(riderId!=null,PharmacyOrder::getRiderId,riderId).ge(startTime!=null,PharmacyOrder::getCreateTime,startTime).le(endTime!=null,PharmacyOrder::getCreateTime,endTime).orderByDesc(PharmacyOrder::getCreateTime);
        addStatusCondition(q,orderStatus);
        if (keyword!=null&&!keyword.isBlank()) {
            List<Long> userIds = userMapper.selectList(new LambdaQueryWrapper<SysUser>().and(w->w.like(SysUser::getUsername,keyword).or().like(SysUser::getPhone,keyword))).stream().map(SysUser::getId).toList();
            q.and(w -> { w.like(PharmacyOrder::getReceiverName,keyword).or().like(PharmacyOrder::getReceiverPhone,keyword); if (!userIds.isEmpty()) w.or().in(PharmacyOrder::getUserId,userIds); });
        }
        orderMapper.selectPage(p,q);
        Map<Long,SysUser> users=userMap(p.getRecords());
        return PageData.from(p,o->toOrderVO(o,users.get(o.getUserId())));
    }

    @Override public OrderDetailVO adminDetail(Long orderId) { PharmacyOrder order=get(orderId); return toDetail(order,userMapper.selectById(order.getUserId())); }
    @Override @Transactional(rollbackFor=Exception.class) public void accept(Long adminId,Long orderId,RemarkRequest request){transition(get(orderId),OrderStatus.PENDING_ACCEPT,OrderStatus.TO_PACK,adminId,blank(request.getAdminRemark()),null);}
    @Override @Transactional(rollbackFor=Exception.class) public void pack(Long adminId,Long orderId,RemarkRequest request){transition(get(orderId),OrderStatus.TO_PACK,OrderStatus.TO_DISPATCH,adminId,blank(request.getAdminRemark()),null);}
    @Override @Transactional(rollbackFor=Exception.class) public void dispatch(Long adminId,Long orderId,DispatchRequest request){DeliveryRider rider=riderMapper.selectById(request.getRiderId());if(rider==null||!Integer.valueOf(1).equals(rider.getStatus()))throw new BusinessException(ErrorCode.PARAM_INVALID,"骑手不存在或当前不可接单");transition(get(orderId),OrderStatus.TO_DISPATCH,OrderStatus.DELIVERING,adminId,blank(request.getAdminRemark()),rider);}
    @Override @Transactional(rollbackFor=Exception.class) public void complete(Long adminId,Long orderId,RemarkRequest request){transition(get(orderId),OrderStatus.DELIVERING,OrderStatus.COMPLETED,adminId,blank(request.getAdminRemark()),null);}
    @Override @Transactional(rollbackFor=Exception.class) public void adminCancel(Long adminId,Long orderId,ReasonRequest request){cancelInternal(get(orderId),EnumSet.of(OrderStatus.PENDING_ACCEPT,OrderStatus.TO_PACK,OrderStatus.TO_DISPATCH),request.getReason(),OperatorType.ADMIN,adminId);}

    private void transition(PharmacyOrder order, OrderStatus expected, OrderStatus target, Long adminId, String remark, DeliveryRider rider) {
        if (order.getOrderStatus() != expected || !OrderStateMachine.canTransition(order.getOrderStatus(),target)) throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT,"当前订单状态不允许该操作");
        order.setOrderStatus(target); if (remark!=null) order.setAdminRemark(remark);
        LocalDateTime now=LocalDateTime.now(); order.setUpdateTime(now);
        if(target==OrderStatus.TO_PACK) order.setAcceptedTime(now);
        if(target==OrderStatus.TO_DISPATCH) order.setPackedTime(now);
        if(target==OrderStatus.DELIVERING){order.setDispatchedTime(now);order.setRiderId(rider.getId());order.setRiderName(rider.getRiderName());order.setRiderPhone(rider.getPhone());}
        if(target==OrderStatus.COMPLETED) order.setCompletedTime(now);
        orderMapper.updateById(order); addLog(order.getId(),expected,target,OperatorType.ADMIN,adminId,remark);
    }

    private void cancelInternal(PharmacyOrder order, Set<OrderStatus> allowed, String reason, OperatorType operatorType, Long operatorId) {
        if (!allowed.contains(order.getOrderStatus()) || !OrderStateMachine.canTransition(order.getOrderStatus(),OrderStatus.CANCELED)) throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT,"当前订单状态不允许取消");
        List<PharmacyOrderItem> items=itemMapper.selectList(new LambdaQueryWrapper<PharmacyOrderItem>().eq(PharmacyOrderItem::getOrderId,order.getId()));
        for(PharmacyOrderItem item:items) if(item.getMedicineId()!=null) medicineMapper.restoreStock(item.getMedicineId(),item.getQuantity());
        OrderStatus before=order.getOrderStatus();order.setOrderStatus(OrderStatus.CANCELED);order.setCancelReason(reason);order.setCanceledTime(LocalDateTime.now());order.setUpdateTime(LocalDateTime.now());if(operatorType==OperatorType.ADMIN)order.setAdminRemark(reason);orderMapper.updateById(order);addLog(order.getId(),before,OrderStatus.CANCELED,operatorType,operatorId,reason);
    }

    private PharmacyOrder get(Long id){PharmacyOrder order=orderMapper.selectById(id);if(order==null)throw new BusinessException(ErrorCode.NOT_FOUND,"订单不存在");return order;}
    private void addLog(Long orderId,OrderStatus before,OrderStatus after,OperatorType type,Long operatorId,String remark){OrderStatusLog log=new OrderStatusLog();log.setOrderId(orderId);log.setBeforeStatus(before);log.setAfterStatus(after);log.setOperatorType(type);log.setOperatorId(operatorId);log.setRemark(remark);log.setCreateTime(LocalDateTime.now());logMapper.insert(log);}
    private void addStatusCondition(LambdaQueryWrapper<PharmacyOrder> q,String status){if(status==null||status.isBlank())return;try{q.eq(PharmacyOrder::getOrderStatus,OrderStatus.valueOf(status));}catch(IllegalArgumentException e){throw new BusinessException(ErrorCode.PARAM_INVALID,"订单状态参数不合法");}}
    private Map<Long,SysUser> userMap(List<PharmacyOrder> orders){List<Long> ids=orders.stream().map(PharmacyOrder::getUserId).distinct().toList();if(ids.isEmpty())return Map.of();return userMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(SysUser::getId,Function.identity()));}
    private OrderVO toOrderVO(PharmacyOrder o,SysUser user){return new OrderVO(o.getId(),o.getOrderNo(),user==null?null:user.getUsername(),o.getUserId(),o.getReceiverName(),o.getReceiverPhone(),o.getReceiverAddress(),o.getProductAmount(),o.getDeliveryFee(),o.getOrderAmount(),o.getOrderStatus(),o.getOrderStatus().getLabel(),o.getRiderId(),o.getRiderName(),o.getRiderPhone(),o.getUserRemark(),o.getAdminRemark(),o.getCancelReason(),o.getCreateTime());}
    private OrderDetailVO toDetail(PharmacyOrder o,SysUser user){
        List<OrderItemVO> items=itemMapper.selectList(new LambdaQueryWrapper<PharmacyOrderItem>().eq(PharmacyOrderItem::getOrderId,o.getId())).stream().map(i->new OrderItemVO(i.getId(),i.getMedicineId(),i.getMedicineName(),i.getMedicineImage(),i.getMedicinePrice(),i.getQuantity(),i.getSubtotalAmount())).toList();
        List<OrderStatusLogVO> logs=logMapper.selectList(new LambdaQueryWrapper<OrderStatusLog>().eq(OrderStatusLog::getOrderId,o.getId()).orderByAsc(OrderStatusLog::getCreateTime)).stream().map(l->new OrderStatusLogVO(l.getId(),l.getBeforeStatus(),l.getBeforeStatus()==null?null:l.getBeforeStatus().getLabel(),l.getAfterStatus(),l.getAfterStatus().getLabel(),l.getOperatorType(),l.getOperatorId(),l.getRemark(),l.getCreateTime())).toList();
        return new OrderDetailVO(o.getId(),o.getOrderNo(),user==null?null:user.getUsername(),o.getUserId(),o.getReceiverName(),o.getReceiverPhone(),o.getReceiverAddress(),o.getProductAmount(),o.getDeliveryFee(),o.getOrderAmount(),o.getOrderStatus(),o.getOrderStatus().getLabel(),o.getRiderId(),o.getRiderName(),o.getRiderPhone(),o.getUserRemark(),o.getAdminRemark(),o.getCancelReason(),o.getAcceptedTime(),o.getPackedTime(),o.getDispatchedTime(),o.getCompletedTime(),o.getCanceledTime(),o.getCreateTime(),items,logs);
    }
    private static String fullAddress(UserAddress a){return String.join("",Arrays.asList(a.getProvince(),a.getCity(),a.getDistrict(),a.getDetailAddress()).stream().filter(Objects::nonNull).toList());}
    private static String blank(String v){return v==null||v.isBlank()?null:v;}
}
