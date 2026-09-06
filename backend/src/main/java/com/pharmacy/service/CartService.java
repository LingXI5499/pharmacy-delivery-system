package com.pharmacy.service;
import com.pharmacy.dto.CartAddRequest;
import com.pharmacy.vo.CartVO;
public interface CartService { CartVO getCart(Long userId); void add(Long userId, CartAddRequest request); void updateQuantity(Long userId,Long cartItemId,Integer quantity); void updateSelected(Long userId,Long cartItemId,Boolean selected); void remove(Long userId,Long cartItemId); int clearInvalid(Long userId); }
