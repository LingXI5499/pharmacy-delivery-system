package com.pharmacy.service;

import com.pharmacy.common.ErrorCode;
import com.pharmacy.dto.AddressRequest;
import com.pharmacy.entity.UserAddress;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.mapper.UserAddressMapper;
import com.pharmacy.service.impl.AddressServiceImpl;
import com.pharmacy.vo.AddressVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressServiceImplTest {
    @Mock private UserAddressMapper addressMapper;
    @InjectMocks private AddressServiceImpl addressService;

    @Test
    void firstAddressBecomesDefault() {
        when(addressMapper.selectCount(any())).thenReturn(0L);
        when(addressMapper.selectList(any())).thenReturn(List.of());
        when(addressMapper.insert(any(UserAddress.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, UserAddress.class).setId(1L);
            return 1;
        });
        AddressVO vo = addressService.add(8L, request(false));
        assertEquals(1, vo.isDefault());
    }

    @Test
    void addDefaultClearsPreviousDefault() {
        UserAddress previous = owned(3L, 8L, 1);
        when(addressMapper.selectCount(any())).thenReturn(1L);
        when(addressMapper.selectList(any())).thenReturn(List.of(previous));
        addressService.add(8L, request(true));
        ArgumentCaptor<UserAddress> captor = ArgumentCaptor.forClass(UserAddress.class);
        verify(addressMapper).updateById(captor.capture());
        assertEquals(0, captor.getValue().getIsDefault());
        verify(addressMapper).insert(any(UserAddress.class));
    }

    @Test
    void updateRejectsAddressOwnedBySomeoneElse() {
        when(addressMapper.selectById(3L)).thenReturn(owned(3L, 99L, 0));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> addressService.update(8L, 3L, request(false)));
        assertEquals(ErrorCode.ADDRESS_NOT_OWNED, ex.getCode());
        verify(addressMapper, never()).updateById(any(UserAddress.class));
    }

    @Test
    void setDefaultAndRemoveRequireOwnership() {
        when(addressMapper.selectById(3L)).thenReturn(owned(3L, 8L, 0));
        when(addressMapper.selectList(any())).thenReturn(List.of());
        addressService.setDefault(8L, 3L);
        verify(addressMapper).updateById(any(UserAddress.class));

        addressService.remove(8L, 3L);
        verify(addressMapper).deleteById(3L);
    }

    @Test
    void listMapsOwnedAddresses() {
        when(addressMapper.selectList(any())).thenReturn(List.of(owned(3L, 8L, 1)));
        List<AddressVO> list = addressService.list(8L);
        assertEquals(1, list.size());
        assertEquals("张三", list.get(0).receiverName());
    }

    private static AddressRequest request(boolean isDefault) {
        AddressRequest request = new AddressRequest();
        request.setReceiverName("张三");
        request.setReceiverPhone("13800000000");
        request.setProvince("测试省");
        request.setCity("");
        request.setDistrict("测试区");
        request.setDetailAddress("1 号");
        request.setIsDefault(isDefault);
        return request;
    }

    private static UserAddress owned(Long id, Long userId, int isDefault) {
        UserAddress address = new UserAddress();
        address.setId(id);
        address.setUserId(userId);
        address.setReceiverName("张三");
        address.setReceiverPhone("13800000000");
        address.setDetailAddress("1 号");
        address.setIsDefault(isDefault);
        return address;
    }
}
