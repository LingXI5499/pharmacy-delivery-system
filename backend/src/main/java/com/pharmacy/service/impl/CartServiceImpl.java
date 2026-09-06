
package com.pharmacy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pharmacy.common.ErrorCode;
import com.pharmacy.dto.CartAddRequest;
import com.pharmacy.entity.Medicine;
import com.pharmacy.entity.ShoppingCart;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.mapper.MedicineMapper;
import com.pharmacy.mapper.ShoppingCartMapper;
import com.pharmacy.service.CartService;
import com.pharmacy.vo.CartItemVO;
import com.pharmacy.vo.CartVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    private final ShoppingCartMapper cartMapper;
    private final MedicineMapper medicineMapper;

    @Override
    public CartVO getCart(Long userId) {
        List<ShoppingCart> carts = cartMapper.selectList(new LambdaQueryWrapper<ShoppingCart>().eq(ShoppingCart::getUserId, userId).orderByDesc(ShoppingCart::getUpdateTime));
        if (carts.isEmpty()) return new CartVO(List.of(), 0, BigDecimal.ZERO);
        Map<Long, Medicine> medicineMap = new HashMap<>();
        for (Medicine medicine : medicineMapper.selectBatchIds(carts.stream().map(ShoppingCart::getMedicineId).toList())) medicineMap.put(medicine.getId(), medicine);
        List<CartItemVO> items = new ArrayList<>();
        int selectedCount = 0;
        BigDecimal selectedAmount = BigDecimal.ZERO;
        for (ShoppingCart cart : carts) {
            Medicine m = medicineMap.get(cart.getMedicineId());
            boolean available = m != null && Integer.valueOf(1).equals(m.getStatus()) && m.getStock() != null && m.getStock() > 0;
            BigDecimal price = m == null ? BigDecimal.ZERO : m.getPrice();
            BigDecimal subtotal = price.multiply(BigDecimal.valueOf(cart.getQuantity()));
            boolean selected = Integer.valueOf(1).equals(cart.getSelected());
            if (selected) { selectedCount += cart.getQuantity(); if (available) selectedAmount = selectedAmount.add(subtotal); }
            items.add(new CartItemVO(cart.getId(), cart.getMedicineId(), m == null ? "商品已失效" : m.getMedicineName(), m == null ? null : m.getImageUrl(), price, m == null ? 0 : m.getStock(), cart.getQuantity(), selected, subtotal, available));
        }
        return new CartVO(items, selectedCount, selectedAmount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(Long userId, CartAddRequest request) {
        Medicine medicine = medicineMapper.selectById(request.getMedicineId());
        checkSellable(medicine, request.getQuantity());
        ShoppingCart cart = cartMapper.selectOne(new LambdaQueryWrapper<ShoppingCart>().eq(ShoppingCart::getUserId, userId).eq(ShoppingCart::getMedicineId, request.getMedicineId()));
        if (cart == null) {
            cart = new ShoppingCart();
            cart.setUserId(userId); cart.setMedicineId(request.getMedicineId()); cart.setQuantity(request.getQuantity()); cart.setSelected(1); cart.setCreateTime(LocalDateTime.now()); cart.setUpdateTime(LocalDateTime.now());
            cartMapper.insert(cart);
        } else {
            int total = cart.getQuantity() + request.getQuantity();
            checkSellable(medicine, total);
            cart.setQuantity(total); cart.setUpdateTime(LocalDateTime.now());
            cartMapper.updateById(cart);
        }
    }

    @Override
    public void updateQuantity(Long userId, Long cartItemId, Integer quantity) {
        ShoppingCart cart = ownedCart(userId, cartItemId);
        Medicine medicine = medicineMapper.selectById(cart.getMedicineId());
        checkSellable(medicine, quantity);
        cart.setQuantity(quantity); cart.setUpdateTime(LocalDateTime.now()); cartMapper.updateById(cart);
    }

    @Override
    public void updateSelected(Long userId, Long cartItemId, Boolean selected) {
        ShoppingCart cart = ownedCart(userId, cartItemId);
        cart.setSelected(Boolean.TRUE.equals(selected) ? 1 : 0); cart.setUpdateTime(LocalDateTime.now()); cartMapper.updateById(cart);
    }

    @Override
    public void remove(Long userId, Long cartItemId) { cartMapper.deleteById(ownedCart(userId, cartItemId).getId()); }

    @Override
    public int clearInvalid(Long userId) {
        CartVO cart = getCart(userId);
        List<Long> invalidIds = cart.items().stream().filter(i -> !i.available()).map(CartItemVO::cartItemId).toList();
        if (invalidIds.isEmpty()) return 0;
        return cartMapper.deleteByIds(invalidIds);
    }

    private ShoppingCart ownedCart(Long userId, Long id) {
        ShoppingCart cart = cartMapper.selectById(id);
        if (cart == null || !Objects.equals(cart.getUserId(), userId)) throw new BusinessException(ErrorCode.NOT_FOUND, "购物车商品不存在");
        return cart;
    }
    private void checkSellable(Medicine m, int quantity) {
        if (m == null || !Integer.valueOf(1).equals(m.getStatus()) || m.getStock() == null || m.getStock() < quantity) {
            throw new BusinessException(ErrorCode.STOCK_OR_STATUS_CONFLICT, "药品库存不足或已下架");
        }
    }
}
