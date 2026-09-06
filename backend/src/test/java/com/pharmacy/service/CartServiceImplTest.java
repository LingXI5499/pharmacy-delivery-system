
package com.pharmacy.service;

import com.pharmacy.dto.CartAddRequest;
import com.pharmacy.entity.Medicine;
import com.pharmacy.entity.ShoppingCart;
import com.pharmacy.mapper.MedicineMapper;
import com.pharmacy.mapper.ShoppingCartMapper;
import com.pharmacy.service.impl.CartServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {
    @Mock private ShoppingCartMapper cartMapper;
    @Mock private MedicineMapper medicineMapper;
    @InjectMocks private CartServiceImpl cartService;

    @Test
    void shouldInsertCartItemWhenMedicineHasEnoughStock() {
        Medicine medicine = new Medicine();
        medicine.setId(1L); medicine.setStatus(1); medicine.setStock(10); medicine.setPrice(new BigDecimal("18.50"));
        when(medicineMapper.selectById(1L)).thenReturn(medicine);
        when(cartMapper.selectOne(any())).thenReturn(null);
        CartAddRequest request = new CartAddRequest(); request.setMedicineId(1L); request.setQuantity(2);
        cartService.add(100L, request);
        ArgumentCaptor<ShoppingCart> captor = ArgumentCaptor.forClass(ShoppingCart.class);
        verify(cartMapper).insert(captor.capture());
        assertEquals(100L, captor.getValue().getUserId());
        assertEquals(2, captor.getValue().getQuantity());
        assertEquals(1, captor.getValue().getSelected());
    }
}
