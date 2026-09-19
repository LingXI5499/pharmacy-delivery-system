package com.pharmacy.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pharmacy.common.ErrorCode;
import com.pharmacy.dto.RiderRequest;
import com.pharmacy.entity.DeliveryRider;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.mapper.DeliveryRiderMapper;
import com.pharmacy.mapper.PharmacyOrderMapper;
import com.pharmacy.service.impl.RiderServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class RiderServiceImplTest {
    @Mock private DeliveryRiderMapper riderMapper;
    @Mock private PharmacyOrderMapper orderMapper;
    @InjectMocks private RiderServiceImpl riderService;

    @Test
    void createRejectsDuplicatePhone() {
        when(riderMapper.selectCount(any())).thenReturn(1L);
        BusinessException ex = assertThrows(BusinessException.class, () -> riderService.create(request("13800000000")));
        assertEquals(ErrorCode.PARAM_INVALID, ex.getCode());
    }

    @Test
    void createAndUpdatePersistRider() {
        when(riderMapper.selectCount(any())).thenReturn(0L);
        when(riderMapper.insert(any(DeliveryRider.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, DeliveryRider.class).setId(2L);
            return 1;
        });
        assertEquals("骑手甲", riderService.create(request("13800000000")).riderName());

        when(riderMapper.selectById(2L)).thenReturn(rider(2L, "13800000000"));
        riderService.update(2L, request("13800000001"));
        verify(riderMapper).updateById(any(DeliveryRider.class));
    }

    @Test
    void updateStatusPersistsFlag() {
        when(riderMapper.selectById(2L)).thenReturn(rider(2L, "13800000000"));
        riderService.updateStatus(2L, 0);
        verify(riderMapper).updateById(any(DeliveryRider.class));
    }

    @Test
    void deleteRejectsRiderStillDelivering() {
        when(riderMapper.selectById(2L)).thenReturn(rider(2L, "13800000000"));
        when(orderMapper.selectCount(any())).thenReturn(1L);
        BusinessException ex = assertThrows(BusinessException.class, () -> riderService.delete(2L));
        assertEquals(ErrorCode.ORDER_STATUS_CONFLICT, ex.getCode());
        verify(riderMapper, never()).deleteById(2L);
    }

    @Test
    void availableAndPageReturnMappedRows() {
        when(riderMapper.selectList(any())).thenReturn(List.of(rider(2L, "13800000000")));
        assertEquals(1, riderService.available().size());
        when(riderMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<DeliveryRider> page = invocation.getArgument(0);
            page.setRecords(List.of(rider(2L, "13800000000")));
            page.setTotal(1);
            return page;
        });
        assertEquals(1, riderService.page(1, 10, "甲", 1).total());
    }

    @Test
    void missingRiderIsNotFound() {
        when(riderMapper.selectById(2L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> riderService.updateStatus(2L, 1));
        assertEquals(ErrorCode.NOT_FOUND, ex.getCode());
    }

    private static RiderRequest request(String phone) {
        RiderRequest request = new RiderRequest();
        request.setRiderName("骑手甲");
        request.setPhone(phone);
        request.setStatus(1);
        request.setRemark(" ");
        return request;
    }

    private static DeliveryRider rider(Long id, String phone) {
        DeliveryRider rider = new DeliveryRider();
        rider.setId(id);
        rider.setRiderName("骑手甲");
        rider.setPhone(phone);
        rider.setStatus(1);
        return rider;
    }
}
