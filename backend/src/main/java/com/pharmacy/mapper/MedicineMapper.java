
package com.pharmacy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pharmacy.entity.Medicine;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MedicineMapper extends BaseMapper<Medicine> {
    @Update("""
        UPDATE medicine
        SET stock = stock - #{quantity}, version = version + 1, update_time = NOW()
        WHERE id = #{medicineId} AND status = 1 AND is_deleted = 0 AND stock >= #{quantity}
        """)
    int decreaseStock(@Param("medicineId") Long medicineId, @Param("quantity") Integer quantity);

    @Update("""
        UPDATE medicine
        SET stock = stock + #{quantity}, version = version + 1, update_time = NOW()
        WHERE id = #{medicineId}
        """)
    int restoreStock(@Param("medicineId") Long medicineId, @Param("quantity") Integer quantity);
}
