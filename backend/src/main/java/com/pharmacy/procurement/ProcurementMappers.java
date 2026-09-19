package com.pharmacy.procurement;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

final class ProcurementMappers { private ProcurementMappers() {} }

@Mapper interface SupplierMapper extends BaseMapper<Supplier> {}
@Mapper interface PurchaseOrderMapper extends BaseMapper<PurchaseOrder> {
    @Select("SELECT * FROM purchase_order WHERE id=#{id} FOR UPDATE") PurchaseOrder lockById(@Param("id") Long id);
}
@Mapper interface PurchaseOrderItemMapper extends BaseMapper<PurchaseOrderItem> {
    @Update("UPDATE purchase_order_item SET received_qty=received_qty+#{qty} WHERE id=#{id} AND received_qty+#{qty}<=ordered_qty")
    int addReceived(@Param("id") Long id,@Param("qty") int qty);
}
@Mapper interface PurchaseReceiptMapper extends BaseMapper<PurchaseReceipt> {}
@Mapper interface PurchaseReceiptItemMapper extends BaseMapper<PurchaseReceiptItem> {}
