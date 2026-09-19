package com.pharmacy.inventory;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface InventoryCountMapper extends BaseMapper<InventoryCount> {
    @Select("SELECT * FROM inventory_count WHERE id=#{id} FOR UPDATE")
    InventoryCount lockById(@Param("id") Long id);

    @Update("""
        UPDATE inventory_count
        SET status = #{next}, completed_by = #{operatorId}, completed_time = #{now}, update_time = #{now}
        WHERE id = #{id} AND status = #{expected}
        """)
    int transitionComplete(@Param("id") Long id,
                           @Param("expected") String expected,
                           @Param("next") String next,
                           @Param("operatorId") Long operatorId,
                           @Param("now") LocalDateTime now);

    @Update("""
        UPDATE inventory_count SET status = #{next}, update_time = NOW()
        WHERE id = #{id} AND status = #{expected}
        """)
    int transitionStatus(@Param("id") Long id,
                         @Param("expected") String expected,
                         @Param("next") String next);

    @Select("""
        SELECT m.id AS medicineId, m.medicine_name AS medicineName, m.stock AS aggregateStock,
               COALESCE(b.batch_sum, 0) AS batchAvailableSum,
               m.stock - COALESCE(b.batch_sum, 0) AS diffQty
        FROM medicine m
        LEFT JOIN (
          SELECT medicine_id, SUM(available_qty) AS batch_sum
          FROM medicine_batch
          WHERE sellable = 1 AND quality_status = 'QUALIFIED' AND expiry_date > CURDATE()
          GROUP BY medicine_id
        ) b ON b.medicine_id = m.id
        WHERE m.is_deleted = 0 AND m.stock <> COALESCE(b.batch_sum, 0)
        ORDER BY ABS(m.stock - COALESCE(b.batch_sum, 0)) DESC, m.id ASC
        """)
    List<Map<String, Object>> findStockMismatches();
}
