
package com.pharmacy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pharmacy.common.ErrorCode;
import com.pharmacy.dto.AddressRequest;
import com.pharmacy.entity.UserAddress;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.mapper.UserAddressMapper;
import com.pharmacy.service.AddressService;
import com.pharmacy.vo.AddressVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {
    private final UserAddressMapper addressMapper;

    @Override
    public List<AddressVO> list(Long userId) {
        return addressMapper.selectList(new LambdaQueryWrapper<UserAddress>().eq(UserAddress::getUserId, userId)
                        .orderByDesc(UserAddress::getIsDefault).orderByDesc(UserAddress::getUpdateTime))
                .stream().map(AddressServiceImpl::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AddressVO add(Long userId, AddressRequest request) {
        boolean noAddress = addressMapper.selectCount(new LambdaQueryWrapper<UserAddress>().eq(UserAddress::getUserId, userId)) == 0;
        if (Boolean.TRUE.equals(request.getIsDefault()) || noAddress) clearDefault(userId);
        UserAddress address = new UserAddress();
        copy(request, address);
        address.setUserId(userId); address.setIsDefault(Boolean.TRUE.equals(request.getIsDefault()) || noAddress ? 1 : 0);
        address.setCreateTime(LocalDateTime.now()); address.setUpdateTime(LocalDateTime.now());
        addressMapper.insert(address);
        return toVO(address);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AddressVO update(Long userId, Long addressId, AddressRequest request) {
        UserAddress address = owned(userId, addressId);
        if (Boolean.TRUE.equals(request.getIsDefault())) clearDefault(userId);
        copy(request, address);
        address.setIsDefault(Boolean.TRUE.equals(request.getIsDefault()) ? 1 : address.getIsDefault());
        address.setUpdateTime(LocalDateTime.now()); addressMapper.updateById(address);
        return toVO(address);
    }

    @Override
    public void remove(Long userId, Long addressId) { addressMapper.deleteById(owned(userId,addressId).getId()); }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long userId, Long addressId) {
        UserAddress address = owned(userId, addressId);
        clearDefault(userId);
        address.setIsDefault(1); address.setUpdateTime(LocalDateTime.now()); addressMapper.updateById(address);
    }

    private void clearDefault(Long userId) {
        List<UserAddress> defaults = addressMapper.selectList(new LambdaQueryWrapper<UserAddress>().eq(UserAddress::getUserId, userId).eq(UserAddress::getIsDefault, 1));
        for (UserAddress item : defaults) { item.setIsDefault(0); item.setUpdateTime(LocalDateTime.now()); addressMapper.updateById(item); }
    }
    private UserAddress owned(Long userId, Long id) {
        UserAddress address = addressMapper.selectById(id);
        if (address == null || !Objects.equals(address.getUserId(), userId)) throw new BusinessException(ErrorCode.ADDRESS_NOT_OWNED, "当前地址不属于登录用户");
        return address;
    }
    private static void copy(AddressRequest r, UserAddress a) { a.setReceiverName(r.getReceiverName()); a.setReceiverPhone(r.getReceiverPhone()); a.setProvince(blank(r.getProvince())); a.setCity(blank(r.getCity())); a.setDistrict(blank(r.getDistrict())); a.setDetailAddress(r.getDetailAddress()); }
    private static String blank(String value) { return value == null || value.isBlank() ? null : value; }
    static AddressVO toVO(UserAddress a) { return new AddressVO(a.getId(),a.getReceiverName(),a.getReceiverPhone(),a.getProvince(),a.getCity(),a.getDistrict(),a.getDetailAddress(),a.getIsDefault(),a.getCreateTime(),a.getUpdateTime()); }
}
