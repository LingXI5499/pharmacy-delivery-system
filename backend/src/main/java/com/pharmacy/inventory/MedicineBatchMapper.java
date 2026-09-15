package com.pharmacy.inventory;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface MedicineBatchMapper extends BaseMapper<MedicineBatch> {
    @Select("SELECT id FROM inventory_location WHERE location_code=#{code} AND status=1")
    Long locationId(@Param("code") String code);

    @Select("SELECT * FROM medicine_batch WHERE id=#{id} FOR UPDATE")
    MedicineBatch lockById(@Param("id") Long id);

    @Select("""
        SELECT * FROM medicine_batch
        WHERE medicine_id = #{medicineId} AND sellable = 1 AND quality_status = 'QUALIFIED'
          AND expiry_date > #{today} AND available_qty > 0
        ORDER BY expiry_date ASC, id ASC FOR UPDATE
        """)
    List<MedicineBatch> selectSellableForUpdate(@Param("medicineId") Long medicineId,
                                                @Param("today") LocalDate today);

    @Update("""
        UPDATE medicine_batch
        SET available_qty = available_qty - #{quantity}, reserved_qty = reserved_qty + #{quantity},
            version = version + 1, update_time = NOW()
        WHERE id = #{batchId} AND available_qty >= #{quantity}
        """)
    int reserve(@Param("batchId") Long batchId, @Param("quantity") int quantity);

    @Update("""
        UPDATE medicine_batch SET reserved_qty = reserved_qty - #{quantity}, version = version + 1, update_time = NOW()
        WHERE id = #{batchId} AND reserved_qty >= #{quantity}
        """)
    int confirm(@Param("batchId") Long batchId, @Param("quantity") int quantity);

    @Update("""
        UPDATE medicine_batch
        SET available_qty = available_qty + #{quantity}, reserved_qty = reserved_qty - #{quantity},
            version = version + 1, update_time = NOW()
        WHERE id = #{batchId} AND reserved_qty >= #{quantity}
        """)
    int release(@Param("batchId") Long batchId, @Param("quantity") int quantity);

    @Select("SELECT * FROM medicine_batch WHERE medicine_id=#{medicineId} AND location_id=#{locationId} AND batch_no=#{batchNo} FOR UPDATE")
    MedicineBatch findBatchForUpdate(@Param("medicineId") Long medicineId,@Param("locationId") Long locationId,@Param("batchNo") String batchNo);

    @Update("UPDATE medicine_batch SET available_qty=available_qty+#{quantity},version=version+1,update_time=NOW() WHERE id=#{batchId}")
    int addAvailable(@Param("batchId") Long batchId,@Param("quantity") int quantity);

    @Update("UPDATE medicine_batch SET available_qty=available_qty+#{delta},version=version+1,update_time=NOW() WHERE id=#{batchId} AND available_qty+#{delta}>=0")
    int adjustAvailable(@Param("batchId") Long batchId,@Param("delta") int delta);
}
