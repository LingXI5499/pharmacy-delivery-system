package com.pharmacy.service;
import com.pharmacy.dto.AddressRequest;
import com.pharmacy.vo.AddressVO;
import java.util.List;
public interface AddressService { List<AddressVO> list(Long userId); AddressVO add(Long userId,AddressRequest request); AddressVO update(Long userId,Long addressId,AddressRequest request); void remove(Long userId,Long addressId); void setDefault(Long userId,Long addressId); }
