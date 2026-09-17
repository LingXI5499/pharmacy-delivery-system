package com.pharmacy.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pharmacy.common.ErrorCode;
import com.pharmacy.dto.AddressRequest;
import com.pharmacy.dto.CartAddRequest;
import com.pharmacy.dto.CategoryRequest;
import com.pharmacy.dto.MedicineRequest;
import com.pharmacy.dto.RiderRequest;
import com.pharmacy.dto.StockAdjustRequest;
import com.pharmacy.entity.DeliveryRider;
import com.pharmacy.entity.Medicine;
import com.pharmacy.entity.MedicineCategory;
import com.pharmacy.entity.PharmacyOrder;
import com.pharmacy.entity.PharmacyOrderItem;
import com.pharmacy.entity.ShoppingCart;
import com.pharmacy.entity.SysUser;
import com.pharmacy.entity.UserAddress;
import com.pharmacy.enums.OrderStatus;
import com.pharmacy.enums.UserRole;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.inventory.InventoryService;
import com.pharmacy.mapper.DeliveryRiderMapper;
import com.pharmacy.mapper.MedicineCategoryMapper;
import com.pharmacy.mapper.MedicineMapper;
import com.pharmacy.mapper.PharmacyOrderItemMapper;
import com.pharmacy.mapper.PharmacyOrderMapper;
import com.pharmacy.mapper.ShoppingCartMapper;
import com.pharmacy.mapper.SysUserMapper;
import com.pharmacy.mapper.UserAddressMapper;
import com.pharmacy.service.impl.AddressServiceImpl;
import com.pharmacy.service.impl.AdminUserServiceImpl;
import com.pharmacy.service.impl.CartServiceImpl;
import com.pharmacy.service.impl.CategoryServiceImpl;
import com.pharmacy.service.impl.DashboardServiceImpl;
import com.pharmacy.service.impl.MedicineServiceImpl;
import com.pharmacy.service.impl.PublicMedicineServiceImpl;
import com.pharmacy.service.impl.RiderServiceImpl;
import com.pharmacy.vo.CartVO;
import com.pharmacy.vo.DashboardSummaryVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupportingServicesTest {
    @Mock private ShoppingCartMapper cartMapper;
    @Mock private MedicineMapper medicineMapper;
    @Mock private UserAddressMapper addressMapper;
    @Mock private MedicineCategoryMapper categoryMapper;
    @Mock private InventoryService inventoryService;
    @Mock private PharmacyOrderMapper orderMapper;
    @Mock private PharmacyOrderItemMapper itemMapper;
    @Mock private SysUserMapper userMapper;
    @Mock private DeliveryRiderMapper riderMapper;

    @InjectMocks private CartServiceImpl cartService;
    @InjectMocks private AddressServiceImpl addressService;
    @InjectMocks private PublicMedicineServiceImpl publicMedicineService;
    @InjectMocks private MedicineServiceImpl medicineService;
    @InjectMocks private CategoryServiceImpl categoryService;
    @InjectMocks private DashboardServiceImpl dashboardService;
    @InjectMocks private AdminUserServiceImpl adminUserService;
    @InjectMocks private RiderServiceImpl riderService;

    @Test
    void cartAddUpdatesExistingAndRejectsShortage() {
        Medicine medicine = medicine(11L, 5);
        when(medicineMapper.selectById(11L)).thenReturn(medicine);
        ShoppingCart existing = new ShoppingCart();
        existing.setId(2L);
        existing.setUserId(7L);
        existing.setMedicineId(11L);
        existing.setQuantity(3);
        when(cartMapper.selectOne(any())).thenReturn(existing);
        CartAddRequest request = new CartAddRequest();
        request.setMedicineId(11L);
        request.setQuantity(1);
        cartService.add(7L, request);
        verify(cartMapper).updateById(existing);
        assertEquals(4, existing.getQuantity());

        request.setQuantity(3);
        BusinessException shortage = assertThrows(BusinessException.class, () -> cartService.add(7L, request));
        assertEquals(ErrorCode.STOCK_OR_STATUS_CONFLICT, shortage.getCode());
    }

    @Test
    void cartGetSelectedAmountIgnoresUnavailable() {
        ShoppingCart selected = cartRow(1L, 11L, 2, 1);
        ShoppingCart invalid = cartRow(2L, 12L, 1, 1);
        when(cartMapper.selectList(any())).thenReturn(List.of(selected, invalid));
        Medicine ok = medicine(11L, 9);
        ok.setPrice(new BigDecimal("10.00"));
        Medicine down = medicine(12L, 0);
        down.setStatus(0);
        when(medicineMapper.selectBatchIds(any())).thenReturn(List.of(ok, down));
        CartVO vo = cartService.getCart(7L);
        assertEquals(3, vo.selectedCount());
        assertEquals(new BigDecimal("20.00"), vo.selectedAmount());
        assertFalse(vo.items().get(1).available());
        when(cartMapper.deleteByIds(any())).thenReturn(1);
        assertEquals(1, cartService.clearInvalid(7L));
    }

    @Test
    void cartOwnedMutationsAndEmptyCart() {
        when(cartMapper.selectList(any())).thenReturn(List.of());
        assertEquals(0, cartService.getCart(7L).selectedCount());
        when(cartMapper.selectById(2L)).thenReturn(null);
        assertEquals(ErrorCode.NOT_FOUND,
                assertThrows(BusinessException.class, () -> cartService.remove(7L, 2L)).getCode());
        ShoppingCart owned = cartRow(2L, 11L, 1, 1);
        when(cartMapper.selectById(2L)).thenReturn(owned);
        when(medicineMapper.selectById(11L)).thenReturn(medicine(11L, 5));
        cartService.updateQuantity(7L, 2L, 2);
        cartService.updateSelected(7L, 2L, false);
        cartService.remove(7L, 2L);
        verify(cartMapper).deleteById(2L);
    }

    @Test
    void addressAddForcesDefaultWhenEmpty() {
        when(addressMapper.selectCount(any())).thenReturn(0L);
        when(addressMapper.selectList(any())).thenReturn(List.of());
        AddressRequest request = addressRequest(false);
        var vo = addressService.add(7L, request);
        assertEquals(1, vo.isDefault());
        addressService.list(7L);
        when(addressMapper.selectById(3L)).thenReturn(ownedAddress(3L, 7L));
        addressService.update(7L, 3L, addressRequest(true));
        addressService.setDefault(7L, 3L);
        addressService.remove(7L, 3L);
        when(addressMapper.selectById(4L)).thenReturn(ownedAddress(4L, 8L));
        assertEquals(ErrorCode.ADDRESS_NOT_OWNED,
                assertThrows(BusinessException.class, () -> addressService.remove(7L, 4L)).getCode());
    }

    @Test
    void publicCatalogAndAdminMedicine() {
        when(categoryMapper.selectList(any())).thenReturn(List.of(category(1L)));
        assertEquals(1, publicMedicineService.listEnabledCategories().size());
        when(medicineMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<Medicine> page = invocation.getArgument(0);
            page.setRecords(List.of(medicine(11L, 2)));
            page.setTotal(1);
            return page;
        });
        when(categoryMapper.selectBatchIds(any())).thenReturn(List.of(category(1L)));
        publicMedicineService.pageMedicines(1, 10, "维", 1L, "priceAsc");
        publicMedicineService.pageMedicines(1, 10, null, null, "priceDesc");
        publicMedicineService.pageMedicines(1, 10, null, null, "stockDesc");
        publicMedicineService.pageMedicines(0, 0, " ", null, "time");
        when(medicineMapper.selectById(11L)).thenReturn(medicine(11L, 2));
        when(categoryMapper.selectById(1L)).thenReturn(category(1L));
        assertEquals("维生素C", publicMedicineService.getMedicine(11L).medicineName());
        when(medicineMapper.selectById(12L)).thenReturn(null);
        assertEquals(ErrorCode.NOT_FOUND,
                assertThrows(BusinessException.class, () -> publicMedicineService.getMedicine(12L)).getCode());

        when(categoryMapper.selectById(1L)).thenReturn(category(1L));
        MedicineRequest req = medicineRequest();
        when(medicineMapper.insert(any(Medicine.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, Medicine.class).setId(11L);
            return 1;
        });
        assertEquals(0, medicineService.create(req).stock());
        when(medicineMapper.selectById(11L)).thenReturn(medicine(11L, 2));
        medicineService.update(11L, req);
        medicineService.updateStatus(11L, 0);
        StockAdjustRequest adjust = new StockAdjustRequest();
        adjust.setBatchId(5L);
        adjust.setAdjustType("INCREMENT");
        adjust.setQuantity(1);
        adjust.setRemark("入库");
        medicineService.adjustStock(11L, adjust);
        verify(inventoryService).adjustBatch(5L, "INCREMENT", 1, "入库", null);
        medicineService.delete(11L);
        medicineService.page(1, 10, "维", 1L, 1, "LOW");
        medicineService.page(1, 10, null, null, null, "OUT");
        medicineService.page(1, 10, null, null, null, "NORMAL");
        medicineService.lowStock(1, 10);
        when(categoryMapper.selectById(9L)).thenReturn(null);
        req.setCategoryId(9L);
        assertEquals(ErrorCode.NOT_FOUND,
                assertThrows(BusinessException.class, () -> medicineService.create(req)).getCode());
    }

    @Test
    void categoryAndRiderAndAdminUser() {
        CategoryRequest cat = new CategoryRequest();
        cat.setCategoryName("感冒药");
        cat.setSortNo(1);
        cat.setStatus(1);
        when(categoryMapper.selectCount(any())).thenReturn(0L);
        categoryService.create(cat);
        when(categoryMapper.selectById(1L)).thenReturn(category(1L));
        when(categoryMapper.selectCount(any())).thenReturn(0L);
        categoryService.update(1L, cat);
        when(medicineMapper.selectCount(any())).thenReturn(1L);
        assertEquals(ErrorCode.CATEGORY_NOT_EMPTY,
                assertThrows(BusinessException.class, () -> categoryService.delete(1L)).getCode());
        when(medicineMapper.selectCount(any())).thenReturn(0L);
        categoryService.delete(1L);
        categoryService.updateStatus(1L, 0);
        when(categoryMapper.selectPage(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
        categoryService.page(1, 10, "感", 1);

        RiderRequest riderReq = new RiderRequest();
        riderReq.setRiderName("张骑手");
        riderReq.setPhone("13800000001");
        riderReq.setStatus(1);
        when(riderMapper.selectCount(any())).thenReturn(0L);
        riderService.create(riderReq);
        when(riderMapper.selectById(3L)).thenReturn(rider(3L));
        riderService.update(3L, riderReq);
        riderService.updateStatus(3L, 0);
        riderService.available();
        when(riderMapper.selectPage(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
        riderService.page(1, 10, "张", 1);
        when(orderMapper.selectCount(any())).thenReturn(1L);
        assertEquals(ErrorCode.ORDER_STATUS_CONFLICT,
                assertThrows(BusinessException.class, () -> riderService.delete(3L)).getCode());
        when(orderMapper.selectCount(any())).thenReturn(0L);
        riderService.delete(3L);
        when(riderMapper.selectCount(any())).thenReturn(1L);
        assertEquals(ErrorCode.PARAM_INVALID,
                assertThrows(BusinessException.class, () -> riderService.create(riderReq)).getCode());

        SysUser user = new SysUser();
        user.setId(8L);
        user.setUsername("alice");
        user.setNickname("A");
        user.setRole(UserRole.USER);
        user.setStatus(1);
        when(userMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<SysUser> page = invocation.getArgument(0);
            page.setRecords(List.of(user));
            page.setTotal(1);
            return page;
        });
        adminUserService.page(1, 10, "ali", "USER", 1);
        assertEquals(ErrorCode.PARAM_INVALID,
                assertThrows(BusinessException.class, () -> adminUserService.page(1, 10, null, "NOPE", null)).getCode());
        when(userMapper.selectById(8L)).thenReturn(user);
        adminUserService.updateStatus(1L, 8L, 0);
        assertEquals(ErrorCode.PARAM_INVALID,
                assertThrows(BusinessException.class, () -> adminUserService.updateStatus(8L, 8L, 0)).getCode());
        when(userMapper.lockById(8L)).thenReturn(user);
        when(userMapper.addRole(8L, "PHARMACIST")).thenReturn(1);
        adminUserService.updateRole(1L, 8L, "PHARMACIST");
        verify(userMapper).clearRoles(8L);
        assertEquals(ErrorCode.PARAM_INVALID,
                assertThrows(BusinessException.class, () -> adminUserService.updateRole(1L, 1L, "USER")).getCode());
    }

    @Test
    void dashboardAggregatesTodayAndTrends() {
        PharmacyOrder completed = order(1L, OrderStatus.COMPLETED, new BigDecimal("20.00"));
        PharmacyOrder packing = order(2L, OrderStatus.TO_PACK, new BigDecimal("8.00"));
        when(orderMapper.selectList(any())).thenReturn(List.of(completed, packing));
        when(medicineMapper.selectCount(any())).thenReturn(3L);
        DashboardSummaryVO summary = dashboardService.summary();
        assertEquals(2, summary.todayOrderCount());
        assertEquals(new BigDecimal("20.00"), summary.todaySalesAmount());
        dashboardService.orderTrend(7);
        dashboardService.salesTrend(7);
        PharmacyOrderItem item = new PharmacyOrderItem();
        item.setOrderId(1L);
        item.setMedicineId(11L);
        item.setMedicineName("维生素C");
        item.setMedicineImage("/a.png");
        item.setQuantity(2);
        item.setSubtotalAmount(new BigDecimal("18.00"));
        when(itemMapper.selectList(any())).thenReturn(List.of(item));
        when(medicineMapper.selectBatchIds(any())).thenReturn(List.of(medicine(11L, 2)));
        when(categoryMapper.selectBatchIds(any())).thenReturn(List.of(category(1L)));
        assertFalse(dashboardService.categoryDistribution(7).isEmpty());
        assertEquals("维生素C", dashboardService.hotMedicines(5).get(0).medicineName());
        when(orderMapper.selectList(any())).thenReturn(List.of());
        assertTrue(dashboardService.hotMedicines(5).isEmpty());
        assertTrue(dashboardService.categoryDistribution(7).isEmpty());
    }

    private static Medicine medicine(Long id, int stock) {
        Medicine medicine = new Medicine();
        medicine.setId(id);
        medicine.setCategoryId(1L);
        medicine.setMedicineName("维生素C");
        medicine.setPrice(new BigDecimal("9.00"));
        medicine.setStock(stock);
        medicine.setWarningStock(5);
        medicine.setStatus(1);
        medicine.setPrescriptionRequired(0);
        return medicine;
    }

    private static ShoppingCart cartRow(Long id, Long medicineId, int qty, int selected) {
        ShoppingCart cart = new ShoppingCart();
        cart.setId(id);
        cart.setUserId(7L);
        cart.setMedicineId(medicineId);
        cart.setQuantity(qty);
        cart.setSelected(selected);
        return cart;
    }

    private static AddressRequest addressRequest(boolean isDefault) {
        AddressRequest request = new AddressRequest();
        request.setReceiverName("张三");
        request.setReceiverPhone("13800000000");
        request.setProvince("测试省");
        request.setCity("测试市");
        request.setDistrict(" ");
        request.setDetailAddress("1 号");
        request.setIsDefault(isDefault);
        return request;
    }

    private static UserAddress ownedAddress(Long id, Long userId) {
        UserAddress address = new UserAddress();
        address.setId(id);
        address.setUserId(userId);
        address.setReceiverName("张三");
        address.setReceiverPhone("13800000000");
        address.setDetailAddress("1 号");
        address.setIsDefault(0);
        return address;
    }

    private static MedicineCategory category(Long id) {
        MedicineCategory category = new MedicineCategory();
        category.setId(id);
        category.setCategoryName("维生素");
        category.setSortNo(1);
        category.setStatus(1);
        return category;
    }

    private static MedicineRequest medicineRequest() {
        MedicineRequest request = new MedicineRequest();
        request.setCategoryId(1L);
        request.setMedicineName("维生素C");
        request.setPrice(new BigDecimal("9.00"));
        request.setStock(0);
        request.setWarningStock(5);
        request.setPrescriptionRequired(0);
        request.setStatus(1);
        request.setDescription(" ");
        return request;
    }

    private static DeliveryRider rider(Long id) {
        DeliveryRider rider = new DeliveryRider();
        rider.setId(id);
        rider.setRiderName("张骑手");
        rider.setPhone("13800000001");
        rider.setStatus(1);
        return rider;
    }

    private static PharmacyOrder order(Long id, OrderStatus status, BigDecimal amount) {
        PharmacyOrder order = new PharmacyOrder();
        order.setId(id);
        order.setOrderStatus(status);
        order.setOrderAmount(amount);
        order.setCreateTime(LocalDateTime.now());
        return order;
    }
}
