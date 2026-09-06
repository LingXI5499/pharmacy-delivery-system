
package com.pharmacy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pharmacy.common.ErrorCode;
import com.pharmacy.common.PageData;
import com.pharmacy.dto.RiderRequest;
import com.pharmacy.entity.DeliveryRider;
import com.pharmacy.entity.PharmacyOrder;
import com.pharmacy.enums.OrderStatus;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.mapper.DeliveryRiderMapper;
import com.pharmacy.mapper.PharmacyOrderMapper;
import com.pharmacy.service.RiderService;
import com.pharmacy.vo.RiderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RiderServiceImpl implements RiderService {
    private final DeliveryRiderMapper riderMapper;
    private final PharmacyOrderMapper orderMapper;
    @Override public PageData<RiderVO> page(long page,long size,String keyword,Integer status){Page<DeliveryRider> p=new Page<>(Math.max(page,1),Math.min(Math.max(size,1),100));riderMapper.selectPage(p,new LambdaQueryWrapper<DeliveryRider>().like(keyword!=null&&!keyword.isBlank(),DeliveryRider::getRiderName,keyword).eq(status!=null,DeliveryRider::getStatus,status).orderByDesc(DeliveryRider::getUpdateTime));return PageData.from(p,RiderServiceImpl::toVO);}
    @Override public List<RiderVO> available(){return riderMapper.selectList(new LambdaQueryWrapper<DeliveryRider>().eq(DeliveryRider::getStatus,1).orderByDesc(DeliveryRider::getId)).stream().map(RiderServiceImpl::toVO).toList();}
    @Override public RiderVO create(RiderRequest r){long exists=riderMapper.selectCount(new LambdaQueryWrapper<DeliveryRider>().eq(DeliveryRider::getPhone,r.getPhone()));if(exists>0)throw new BusinessException(ErrorCode.PARAM_INVALID,"骑手手机号已存在");DeliveryRider item=new DeliveryRider();copy(r,item);item.setIsDeleted(0);item.setCreateTime(LocalDateTime.now());item.setUpdateTime(LocalDateTime.now());riderMapper.insert(item);return toVO(item);}
    @Override public RiderVO update(Long id,RiderRequest r){DeliveryRider item=get(id);long exists=riderMapper.selectCount(new LambdaQueryWrapper<DeliveryRider>().eq(DeliveryRider::getPhone,r.getPhone()).ne(DeliveryRider::getId,id));if(exists>0)throw new BusinessException(ErrorCode.PARAM_INVALID,"骑手手机号已存在");copy(r,item);item.setUpdateTime(LocalDateTime.now());riderMapper.updateById(item);return toVO(item);}
    @Override public void updateStatus(Long id,Integer status){DeliveryRider item=get(id);item.setStatus(status);item.setUpdateTime(LocalDateTime.now());riderMapper.updateById(item);}
    @Override public void delete(Long id){get(id);long delivering=orderMapper.selectCount(new LambdaQueryWrapper<PharmacyOrder>().eq(PharmacyOrder::getRiderId,id).eq(PharmacyOrder::getOrderStatus,OrderStatus.DELIVERING));if(delivering>0)throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT,"骑手存在配送中的订单，不能删除");riderMapper.deleteById(id);}
    private DeliveryRider get(Long id){DeliveryRider item=riderMapper.selectById(id);if(item==null)throw new BusinessException(ErrorCode.NOT_FOUND,"骑手不存在");return item;}
    private static void copy(RiderRequest r,DeliveryRider item){item.setRiderName(r.getRiderName());item.setPhone(r.getPhone());item.setStatus(r.getStatus());item.setRemark(r.getRemark()==null||r.getRemark().isBlank()?null:r.getRemark());}
    static RiderVO toVO(DeliveryRider r){return new RiderVO(r.getId(),r.getRiderName(),r.getPhone(),r.getStatus(),r.getRemark(),r.getCreateTime(),r.getUpdateTime());}
}
