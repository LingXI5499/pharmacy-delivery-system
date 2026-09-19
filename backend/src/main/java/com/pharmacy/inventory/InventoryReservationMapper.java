package com.pharmacy.inventory;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface InventoryReservationMapper extends BaseMapper<InventoryReservation> {
    @Select("SELECT * FROM inventory_reservation WHERE order_id=#{orderId} AND status='ACTIVE' ORDER BY id FOR UPDATE")
    List<InventoryReservation> activeForUpdate(@Param("orderId") Long orderId);
    @Select("SELECT * FROM inventory_reservation WHERE order_id=#{orderId} AND status='COMMITTED' ORDER BY id FOR UPDATE")
    List<InventoryReservation> committedForUpdate(@Param("orderId") Long orderId);
}
