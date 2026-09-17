package com.pharmacy.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pharmacy.common.ErrorCode;
import com.pharmacy.dto.DispatchRequest;
import com.pharmacy.dto.OrderCreateRequest;
import com.pharmacy.dto.ReasonRequest;
import com.pharmacy.dto.RemarkRequest;
import com.pharmacy.entity.DeliveryRider;
import com.pharmacy.entity.Medicine;
import com.pharmacy.entity.OrderStatusLog;
import com.pharmacy.entity.PharmacyOrder;
import com.pharmacy.entity.PharmacyOrderItem;
import com.pharmacy.entity.ShoppingCart;
import com.pharmacy.entity.SysUser;
import com.pharmacy.entity.UserAddress;
import com.pharmacy.enums.OperatorType;
import com.pharmacy.enums.OrderStatus;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.inventory.InventoryService;
import com.pharmacy.mapper.DeliveryRiderMapper;
import com.pharmacy.mapper.MedicineMapper;
import com.pharmacy.mapper.OrderStatusLogMapper;
import com.pharmacy.mapper.PharmacyOrderItemMapper;
import com.pharmacy.mapper.PharmacyOrderMapper;
import com.pharmacy.mapper.ShoppingCartMapper;
import com.pharmacy.mapper.SysUserMapper;
import com.pharmacy.mapper.UserAddressMapper;
import com.pharmacy.messaging.OrderPaymentPendingEvent;
import com.pharmacy.payment.RefundService;
import com.pharmacy.prescription.PrescriptionService;
import com.pharmacy.service.impl.OrderServiceImpl;
import com.pharmacy.support.MybatisPlusLambdaInit;
import com.pharmacy.vo.OrderDetailVO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {
    @BeforeAll
    static void initLambdaCache() {
        MybatisPlusLambdaInit.entities(ShoppingCart.class, PharmacyOrder.class, PharmacyOrderItem.class,
                OrderStatusLog.class, SysUser.class);
    }

    @Mock private PharmacyOrderMapper orderMapper;
    @Mock private PharmacyOrderItemMapper itemMapper;
    @Mock private OrderStatusLogMapper logMapper;
    @Mock private UserAddressMapper addressMapper;
    @Mock private ShoppingCartMapper cartMapper;
    @Mock private MedicineMapper medicineMapper;
    @Mock private DeliveryRiderMapper riderMapper;
    @Mock private SysUserMapper userMapper;
    @Mock private InventoryService inventoryService;
    @Mock private PrescriptionService prescriptionService;
    @Mock private ApplicationEventPublisher events;
    @Mock private RefundService refundService;
    @InjectMocks private OrderServiceImpl orderService;

    @BeforeEach
    void injectOrderConfig() {
        ReflectionTestUtils.setField(orderService, "deliveryFee", new BigDecimal("5.00"));
        ReflectionTestUtils.setField(orderService, "paymentTimeout", Duration.ofMinutes(30));
    }

    @Test
    void createRejectsBlankIdempotencyKey() {
        OrderCreateRequest request = otcRequest();
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.create(8L, request, "  "));
        assertEquals(ErrorCode.PARAM_INVALID, ex.getCode());
        verify(orderMapper, never()).insert(any(PharmacyOrder.class));
    }

    @Test
    void createReplaysExistingIdempotentOrder() {
        PharmacyOrder existing = order(40L, 8L, OrderStatus.PENDING_PAYMENT);
        existing.setOrderAmount(new BigDecimal("23.00"));
        when(userMapper.lockById(8L)).thenReturn(user(8L));
        when(orderMapper.findByIdempotencyKey(8L, "same-key")).thenReturn(existing);

        Map<String, Object> result = orderService.create(8L, otcRequest(), "same-key");

        assertEquals(40L, result.get("orderId"));
        assertEquals("O40", result.get("orderNo"));
        verify(orderMapper, never()).insert(any(PharmacyOrder.class));
        verify(inventoryService, never()).reserve(any(), anyList(), any(), any());
    }

    @Test
    void createRejectsAddressNotOwned() {
        when(userMapper.lockById(8L)).thenReturn(user(8L));
        when(orderMapper.findByIdempotencyKey(8L, "k1")).thenReturn(null);
        UserAddress address = address(1L, 99L);
        when(addressMapper.selectById(1L)).thenReturn(address);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.create(8L, otcRequest(), "k1"));
        assertEquals(ErrorCode.ADDRESS_NOT_OWNED, ex.getCode());
    }

    @Test
    void createRejectsCartItemsBelongingToAnotherUser() {
        when(userMapper.lockById(8L)).thenReturn(user(8L));
        when(orderMapper.findByIdempotencyKey(8L, "k2")).thenReturn(null);
        when(addressMapper.selectById(1L)).thenReturn(address(1L, 8L));
        when(cartMapper.selectList(any())).thenReturn(List.of());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.create(8L, otcRequest(), "k2"));
        assertEquals(ErrorCode.PARAM_INVALID, ex.getCode());
    }

    @Test
    void createRejectsOffShelfMedicine() {
        stubOwnedCartAndAddress();
        Medicine medicine = medicine(9L, 0, 0);
        when(medicineMapper.selectBatchIds(anyList())).thenReturn(List.of(medicine));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.create(8L, otcRequest(), "k3"));
        assertEquals(ErrorCode.STOCK_OR_STATUS_CONFLICT, ex.getCode());
    }

    @Test
    void createRequiresPrescriptionForRxMedicine() {
        stubOwnedCartAndAddress();
        Medicine medicine = medicine(9L, 1, 1);
        when(medicineMapper.selectBatchIds(anyList())).thenReturn(List.of(medicine));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.create(8L, otcRequest(), "k4"));
        assertEquals(ErrorCode.PARAM_INVALID, ex.getCode());
        verify(inventoryService, never()).reserve(any(), anyList(), any(), any());
    }

    @Test
    void createOtcReservesStockAndPublishesTimeoutEvent() {
        stubOwnedCartAndAddress();
        Medicine medicine = medicine(9L, 1, 0);
        when(medicineMapper.selectBatchIds(anyList())).thenReturn(List.of(medicine));
        when(orderMapper.insert(any(PharmacyOrder.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, PharmacyOrder.class).setId(70L);
            return 1;
        });

        Map<String, Object> result = orderService.create(8L, otcRequest(), "otc-1");

        assertEquals(70L, result.get("orderId"));
        assertEquals(OrderStatus.PENDING_PAYMENT, result.get("orderStatus"));
        ArgumentCaptor<PharmacyOrder> orderCaptor = ArgumentCaptor.forClass(PharmacyOrder.class);
        verify(orderMapper).insert(orderCaptor.capture());
        assertEquals(new BigDecimal("41.00"), orderCaptor.getValue().getOrderAmount());
        verify(inventoryService).reserve(eq(70L), anyList(), any(LocalDateTime.class), eq(8L));
        verify(events).publishEvent(any(OrderPaymentPendingEvent.class));
        verify(cartMapper).deleteByIds(List.of(2L));
        verify(prescriptionService, never()).attach(any(), any());
    }

    @Test
    void createPrescriptionSkipsReserveUntilReview() {
        stubOwnedCartAndAddress();
        Medicine medicine = medicine(9L, 1, 1);
        when(medicineMapper.selectBatchIds(anyList())).thenReturn(List.of(medicine));
        when(orderMapper.insert(any(PharmacyOrder.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, PharmacyOrder.class).setId(71L);
            return 1;
        });
        OrderCreateRequest request = otcRequest();
        request.setPrescriptionId(55L);

        Map<String, Object> result = orderService.create(8L, request, "rx-1");

        assertEquals(OrderStatus.PENDING_REVIEW, result.get("orderStatus"));
        verify(prescriptionService).validateForOrder(eq(55L), eq(8L), anyList());
        verify(prescriptionService).attach(55L, 71L);
        verify(inventoryService, never()).reserve(any(), anyList(), any(), any());
        verify(events, never()).publishEvent(any());
    }

    @Test
    void userDetailRejectsOtherUsersOrder() {
        when(orderMapper.selectById(3L)).thenReturn(order(3L, 8L, OrderStatus.TO_PACK));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.userDetail(99L, 3L));
        assertEquals(ErrorCode.FORBIDDEN, ex.getCode());
    }

    @Test
    void userDetailReturnsItemsAndLogs() {
        PharmacyOrder order = order(3L, 8L, OrderStatus.TO_PACK);
        when(orderMapper.selectById(3L)).thenReturn(order);
        when(itemMapper.selectList(any())).thenReturn(List.of());
        when(logMapper.selectList(any())).thenReturn(List.of());

        OrderDetailVO detail = orderService.userDetail(8L, 3L);
        assertEquals(3L, detail.id());
        assertEquals("待打包", detail.orderStatusName());
        assertNull(detail.username());
    }

    @Test
    void userCancelReleasesReservation() {
        when(orderMapper.selectById(4L)).thenReturn(order(4L, 8L, OrderStatus.PENDING_PAYMENT));
        ReasonRequest reason = new ReasonRequest();
        reason.setReason("不想要了");

        orderService.userCancel(8L, 4L, reason);

        verify(inventoryService).release(4L, "不想要了", 8L);
        ArgumentCaptor<PharmacyOrder> captor = ArgumentCaptor.forClass(PharmacyOrder.class);
        verify(orderMapper).updateById(captor.capture());
        assertEquals(OrderStatus.CANCELED, captor.getValue().getOrderStatus());
    }

    @Test
    void userCancelRejectsPackedOrder() {
        when(orderMapper.selectById(4L)).thenReturn(order(4L, 8L, OrderStatus.TO_PACK));
        ReasonRequest reason = new ReasonRequest();
        reason.setReason("太晚了");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.userCancel(8L, 4L, reason));
        assertEquals(ErrorCode.ORDER_STATUS_CONFLICT, ex.getCode());
        verify(inventoryService, never()).release(any(), any(), any());
    }

    @Test
    void packTransitionsToDispatch() {
        when(orderMapper.selectById(5L)).thenReturn(order(5L, 8L, OrderStatus.TO_PACK));
        orderService.pack(1L, 5L, new RemarkRequest());
        ArgumentCaptor<PharmacyOrder> captor = ArgumentCaptor.forClass(PharmacyOrder.class);
        verify(orderMapper).updateById(captor.capture());
        assertEquals(OrderStatus.TO_DISPATCH, captor.getValue().getOrderStatus());
    }

    @Test
    void dispatchRejectsInactiveRider() {
        DeliveryRider rider = new DeliveryRider();
        rider.setId(2L);
        rider.setStatus(0);
        when(riderMapper.selectById(2L)).thenReturn(rider);
        DispatchRequest request = new DispatchRequest();
        request.setRiderId(2L);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.dispatch(1L, 6L, request));
        assertEquals(ErrorCode.PARAM_INVALID, ex.getCode());
    }

    @Test
    void dispatchAssignsRider() {
        when(orderMapper.selectById(6L)).thenReturn(order(6L, 8L, OrderStatus.TO_DISPATCH));
        DeliveryRider rider = new DeliveryRider();
        rider.setId(2L);
        rider.setStatus(1);
        rider.setRiderName("骑手甲");
        rider.setPhone("13800000000");
        when(riderMapper.selectById(2L)).thenReturn(rider);
        DispatchRequest request = new DispatchRequest();
        request.setRiderId(2L);

        orderService.dispatch(1L, 6L, request);

        ArgumentCaptor<PharmacyOrder> captor = ArgumentCaptor.forClass(PharmacyOrder.class);
        verify(orderMapper).updateById(captor.capture());
        assertEquals(OrderStatus.DELIVERING, captor.getValue().getOrderStatus());
        assertEquals(2L, captor.getValue().getRiderId());
        assertEquals("骑手甲", captor.getValue().getRiderName());
    }

    @Test
    void completeMarksDeliveredOrder() {
        when(orderMapper.selectById(7L)).thenReturn(order(7L, 8L, OrderStatus.DELIVERING));
        orderService.complete(1L, 7L, new RemarkRequest());
        ArgumentCaptor<PharmacyOrder> captor = ArgumentCaptor.forClass(PharmacyOrder.class);
        verify(orderMapper).updateById(captor.capture());
        assertEquals(OrderStatus.COMPLETED, captor.getValue().getOrderStatus());
    }

    @Test
    void adminCancelPackedOrderStartsRefund() {
        when(orderMapper.selectById(8L)).thenReturn(order(8L, 8L, OrderStatus.TO_PACK));
        ReasonRequest reason = new ReasonRequest();
        reason.setReason("缺货");
        orderService.adminCancel(1L, 8L, reason);
        verify(refundService).requestByAdmin(1L, 8L, "缺货");
        verify(inventoryService, never()).release(any(), any(), any());
    }

    @Test
    void adminCancelPendingPaymentReleasesStock() {
        when(orderMapper.selectById(9L)).thenReturn(order(9L, 8L, OrderStatus.PENDING_PAYMENT));
        ReasonRequest reason = new ReasonRequest();
        reason.setReason("用户电话取消");
        orderService.adminCancel(1L, 9L, reason);
        verify(inventoryService).release(9L, "用户电话取消", 1L);
    }

    @Test
    void pageUserOrdersRejectsIllegalStatus() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.pageUserOrders(8L, 1, 10, "NOT_A_STATUS"));
        assertEquals(ErrorCode.PARAM_INVALID, ex.getCode());
    }

    @Test
    void pageUserOrdersFiltersStatus() {
        when(orderMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<PharmacyOrder> page = invocation.getArgument(0);
            page.setRecords(List.of(order(1L, 8L, OrderStatus.TO_PACK)));
            page.setTotal(1);
            return page;
        });
        assertEquals(1, orderService.pageUserOrders(8L, 1, 10, "TO_PACK").total());
    }

    @Test
    void pageAdminOrdersSearchesKeyword() {
        when(userMapper.selectList(any())).thenReturn(List.of(user(8L)));
        when(orderMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<PharmacyOrder> page = invocation.getArgument(0);
            page.setRecords(List.of(order(1L, 8L, OrderStatus.TO_PACK)));
            page.setTotal(1);
            return page;
        });
        assertEquals(1, orderService.pageAdminOrders(1, 10, "O", "TO_PACK", "ali", 2L,
                LocalDateTime.now().minusDays(1), LocalDateTime.now()).total());
    }

    @Test
    void missingOrderIsNotFound() {
        when(orderMapper.selectById(404L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> orderService.adminDetail(404L));
        assertEquals(ErrorCode.NOT_FOUND, ex.getCode());
    }

    @Test
    void acceptLegacyPendingAcceptOrder() {
        when(orderMapper.selectById(11L)).thenReturn(order(11L, 8L, OrderStatus.PENDING_ACCEPT));
        RemarkRequest remark = new RemarkRequest();
        remark.setAdminRemark("接单");
        orderService.accept(1L, 11L, remark);
        ArgumentCaptor<PharmacyOrder> captor = ArgumentCaptor.forClass(PharmacyOrder.class);
        verify(orderMapper).updateById(captor.capture());
        assertEquals(OrderStatus.TO_PACK, captor.getValue().getOrderStatus());
        assertEquals("接单", captor.getValue().getAdminRemark());
    }

    @Test
    void createRejectsMissingUserLongKeyAndMissingAddress() {
        OrderCreateRequest request = otcRequest();
        request.setUserRemark("  ");
        assertEquals(ErrorCode.PARAM_INVALID,
                assertThrows(BusinessException.class, () -> orderService.create(8L, request, "k".repeat(81))).getCode());
        when(userMapper.lockById(8L)).thenReturn(null);
        assertEquals(ErrorCode.UNAUTHORIZED,
                assertThrows(BusinessException.class, () -> orderService.create(8L, request, "k5")).getCode());
        when(userMapper.lockById(8L)).thenReturn(user(8L));
        when(orderMapper.findByIdempotencyKey(8L, "k6")).thenReturn(null);
        when(addressMapper.selectById(1L)).thenReturn(null);
        assertEquals(ErrorCode.ADDRESS_NOT_OWNED,
                assertThrows(BusinessException.class, () -> orderService.create(8L, request, "k6")).getCode());
    }

    @Test
    void createRejectsMissingMedicineAndCancelForeignOrder() {
        stubOwnedCartAndAddress();
        when(medicineMapper.selectBatchIds(anyList())).thenReturn(List.of());
        assertEquals(ErrorCode.STOCK_OR_STATUS_CONFLICT,
                assertThrows(BusinessException.class, () -> orderService.create(8L, otcRequest(), "k7")).getCode());

        when(orderMapper.selectById(4L)).thenReturn(order(4L, 8L, OrderStatus.PENDING_PAYMENT));
        ReasonRequest reason = new ReasonRequest();
        reason.setReason("越权");
        assertEquals(ErrorCode.FORBIDDEN,
                assertThrows(BusinessException.class, () -> orderService.userCancel(99L, 4L, reason)).getCode());
    }

    @Test
    void pageAdminOrdersCoversEmptyKeywordAndDetailLogs() {
        doAnswer(invocation -> {
            Page<PharmacyOrder> page = invocation.getArgument(0);
            page.setRecords(List.of());
            page.setTotal(0);
            return page;
        }).when(orderMapper).selectPage(any(), any());
        assertEquals(0, orderService.pageAdminOrders(0, 0, " ", null, "  ", null, null, null).total());

        when(userMapper.selectList(any())).thenReturn(List.of());
        doAnswer(invocation -> {
            Page<PharmacyOrder> page = invocation.getArgument(0);
            page.setRecords(List.of(order(1L, 8L, OrderStatus.TO_PACK)));
            page.setTotal(1);
            return page;
        }).when(orderMapper).selectPage(any(), any());
        when(userMapper.selectBatchIds(anyList())).thenReturn(List.of(user(8L)));
        assertEquals(1, orderService.pageAdminOrders(1, 10, null, null, "nobody", null, null, null).total());

        PharmacyOrder packed = order(12L, 8L, OrderStatus.TO_PACK);
        when(orderMapper.selectById(12L)).thenReturn(packed);
        when(userMapper.selectById(8L)).thenReturn(user(8L));
        PharmacyOrderItem line = new PharmacyOrderItem();
        line.setId(1L);
        line.setMedicineId(9L);
        line.setMedicineName("虚构感冒颗粒");
        line.setQuantity(1);
        line.setMedicinePrice(new BigDecimal("18.00"));
        line.setSubtotalAmount(new BigDecimal("18.00"));
        when(itemMapper.selectList(any())).thenReturn(List.of(line));
        OrderStatusLog created = new OrderStatusLog();
        created.setId(1L);
        created.setBeforeStatus(null);
        created.setAfterStatus(OrderStatus.PENDING_PAYMENT);
        created.setOperatorType(OperatorType.SYSTEM);
        created.setCreateTime(LocalDateTime.now());
        OrderStatusLog packedLog = new OrderStatusLog();
        packedLog.setId(2L);
        packedLog.setBeforeStatus(OrderStatus.TO_PACK);
        packedLog.setAfterStatus(OrderStatus.TO_DISPATCH);
        packedLog.setOperatorType(OperatorType.ADMIN);
        packedLog.setCreateTime(LocalDateTime.now());
        when(logMapper.selectList(any())).thenReturn(List.of(created, packedLog));
        OrderDetailVO detail = orderService.adminDetail(12L);
        assertEquals("alice", detail.username());
        assertEquals(1, detail.items().size());
        assertEquals(2, detail.statusLogs().size());
        assertNull(detail.statusLogs().get(0).beforeStatusName());
        assertEquals("待打包", detail.statusLogs().get(1).beforeStatusName());
    }

    @Test
    void dispatchAndTransitionRejectInvalidState() {
        when(riderMapper.selectById(2L)).thenReturn(null);
        DispatchRequest request = new DispatchRequest();
        request.setRiderId(2L);
        assertEquals(ErrorCode.PARAM_INVALID,
                assertThrows(BusinessException.class, () -> orderService.dispatch(1L, 6L, request)).getCode());

        when(orderMapper.selectById(5L)).thenReturn(order(5L, 8L, OrderStatus.PENDING_PAYMENT));
        assertEquals(ErrorCode.ORDER_STATUS_CONFLICT,
                assertThrows(BusinessException.class, () -> orderService.pack(1L, 5L, new RemarkRequest())).getCode());
    }

    @Test
    void createOtcKeepsUserRemark() {
        stubOwnedCartAndAddress();
        Medicine medicine = medicine(9L, 1, 0);
        when(medicineMapper.selectBatchIds(anyList())).thenReturn(List.of(medicine));
        when(orderMapper.insert(any(PharmacyOrder.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, PharmacyOrder.class).setId(72L);
            return 1;
        });
        OrderCreateRequest request = otcRequest();
        request.setUserRemark("尽快送达");
        orderService.create(8L, request, "otc-remark");
        ArgumentCaptor<PharmacyOrder> captor = ArgumentCaptor.forClass(PharmacyOrder.class);
        verify(orderMapper).insert(captor.capture());
        assertEquals("尽快送达", captor.getValue().getUserRemark());
    }

    private void stubOwnedCartAndAddress() {
        when(userMapper.lockById(8L)).thenReturn(user(8L));
        when(orderMapper.findByIdempotencyKey(eq(8L), any())).thenReturn(null);
        when(addressMapper.selectById(1L)).thenReturn(address(1L, 8L));
        ShoppingCart cart = new ShoppingCart();
        cart.setId(2L);
        cart.setUserId(8L);
        cart.setMedicineId(9L);
        cart.setQuantity(2);
        when(cartMapper.selectList(any())).thenReturn(List.of(cart));
    }

    private static OrderCreateRequest otcRequest() {
        OrderCreateRequest request = new OrderCreateRequest();
        request.setAddressId(1L);
        request.setCartItemIds(List.of(2L));
        return request;
    }

    private static UserAddress address(Long id, Long userId) {
        UserAddress address = new UserAddress();
        address.setId(id);
        address.setUserId(userId);
        address.setReceiverName("张三");
        address.setReceiverPhone("13800000000");
        address.setProvince("测试省");
        address.setCity("测试市");
        address.setDistrict("测试区");
        address.setDetailAddress("1 号");
        return address;
    }

    private static Medicine medicine(Long id, int status, int prescriptionRequired) {
        Medicine medicine = new Medicine();
        medicine.setId(id);
        medicine.setStatus(status);
        medicine.setPrescriptionRequired(prescriptionRequired);
        medicine.setPrice(new BigDecimal("18.00"));
        medicine.setMedicineName("虚构感冒颗粒");
        medicine.setImageUrl("http://example.test/m.png");
        medicine.setStock(20);
        return medicine;
    }

    private static PharmacyOrder order(Long id, Long userId, OrderStatus status) {
        PharmacyOrder order = new PharmacyOrder();
        order.setId(id);
        order.setOrderNo("O" + id);
        order.setUserId(userId);
        order.setOrderStatus(status);
        order.setProductAmount(new BigDecimal("18.00"));
        order.setDeliveryFee(new BigDecimal("5.00"));
        order.setOrderAmount(new BigDecimal("23.00"));
        order.setCreateTime(LocalDateTime.now());
        return order;
    }

    private static SysUser user(Long id) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername("alice");
        return user;
    }
}
