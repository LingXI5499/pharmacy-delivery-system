package com.pharmacy.service;
import com.pharmacy.common.PageData;
import com.pharmacy.dto.RiderRequest;
import com.pharmacy.vo.RiderVO;
import java.util.List;
public interface RiderService { PageData<RiderVO> page(long page,long size,String keyword,Integer status); List<RiderVO> available(); RiderVO create(RiderRequest request); RiderVO update(Long id,RiderRequest request); void updateStatus(Long id,Integer status); void delete(Long id); }
