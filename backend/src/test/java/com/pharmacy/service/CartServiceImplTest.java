package com.pharmacy.service;

import com.pharmacy.common.ErrorCode;
import com.pharmacy.dto.CartAddRequest;
import com.pharmacy.entity.Medicine;
import com.pharmacy.entity.ShoppingCart;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.mapper.MedicineMapper;
import com.pharmacy.mapper.ShoppingCartMapper;
import com.pharmacy.service.impl.CartServiceImpl;
import com.pharmacy.vo.CartVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {
    @Mock private ShoppingCartMapper cartMapper;
    @Mock private MedicineMapper medicineMapper;
    @InjectMocks private CartServiceImpl cartService;

    @Test
    void shouldInsertCartItemWhenMedicineHasEnoughStock() {
        when(medicineMapper.selectById(1L)).thenReturn(sellable());
        when(cartMapper.selectOne(any())).thenReturn(null);
        CartAddRequest request = new CartAddRequest();
        request.setMedicineId(1L);
        request.setQuantity(2);
        cartService.add(100L, request);
        ArgumentCaptor<ShoppingCart> captor = ArgumentCaptor.forClass(ShoppingCart.class);
        verify(cartMapper).insert(captor.capture());
        assertEquals(100L, captor.getValue().getUserId());
        assertEquals(2, captor.getValue().getQuantity());
        assertEquals(1, captor.getValue().getSelected());
    }

    @Test
    void getCartMarksUnavailableItemsAndSumsSelected() {
        ShoppingCart selected = cart(1L, 9L, 2, 1);
        ShoppingCart invalid = cart(2L, 8L, 1, 1);
        when(cartMapper.selectList(any())).thenReturn(List.of(selected, invalid));
        Medicine medicine = sellable();
        medicine.setId(9L);
        medicine.setMedicineName("虚构感冒颗粒");
        medicine.setPrescriptionRequired(0);
        medicine.setPrice(new BigDecimal("5.00"));
        when(medicineMapper.selectBatchIds(anyList())).thenReturn(List.of(medicine));
        CartVO vo = cartService.getCart(100L);
        assertEquals(3, vo.selectedCount());
        assertEquals(new BigDecimal("10.00"), vo.selectedAmount());
        when(cartMapper.deleteByIds(List.of(2L))).thenReturn(1);
        assertEquals(1, cartService.clearInvalid(100L));
    }

    @Test
    void addIncrementsExistingCartRow() {
        when(medicineMapper.selectById(1L)).thenReturn(sellable());
        ShoppingCart existing = cart(9L, 1L, 1, 1);
        when(cartMapper.selectOne(any())).thenReturn(existing);
        CartAddRequest request = new CartAddRequest();
        request.setMedicineId(1L);
        request.setQuantity(2);
        cartService.add(100L, request);
        verify(cartMapper).updateById(existing);
        assertEquals(3, existing.getQuantity());
    }

    @Test
    void addRejectsInsufficientStock() {
        Medicine medicine = sellable();
        medicine.setStock(1);
        when(medicineMapper.selectById(1L)).thenReturn(medicine);
        CartAddRequest request = new CartAddRequest();
        request.setMedicineId(1L);
        request.setQuantity(2);
        BusinessException ex = assertThrows(BusinessException.class, () -> cartService.add(100L, request));
        assertEquals(ErrorCode.STOCK_OR_STATUS_CONFLICT, ex.getCode());
    }

    @Test
    void ownedCartMutationsRequireOwnership() {
        when(cartMapper.selectById(9L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> cartService.remove(100L, 9L));
        ShoppingCart cart = cart(9L, 1L, 1, 1);
        when(cartMapper.selectById(9L)).thenReturn(cart);
        when(medicineMapper.selectById(1L)).thenReturn(sellable());
        cartService.updateQuantity(100L, 9L, 2);
        cartService.updateSelected(100L, 9L, false);
        cartService.remove(100L, 9L);
        verify(cartMapper).deleteById(9L);
    }

    @Test
    void emptyCartReturnsZeroTotals() {
        when(cartMapper.selectList(any())).thenReturn(List.of());
        CartVO vo = cartService.getCart(100L);
        assertEquals(0, vo.selectedCount());
        assertEquals(BigDecimal.ZERO, vo.selectedAmount());
        assertEquals(0, cartService.clearInvalid(100L));
    }

    private static ShoppingCart cart(Long id, Long medicineId, int qty, int selected) {
        ShoppingCart cart = new ShoppingCart();
        cart.setId(id);
        cart.setUserId(100L);
        cart.setMedicineId(medicineId);
        cart.setQuantity(qty);
        cart.setSelected(selected);
        return cart;
    }

    private static Medicine sellable() {
        Medicine medicine = new Medicine();
        medicine.setId(1L);
        medicine.setStatus(1);
        medicine.setStock(10);
        medicine.setPrice(new BigDecimal("18.50"));
        return medicine;
    }
}
