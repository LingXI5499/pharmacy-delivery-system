package com.pharmacy.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pharmacy.entity.PharmacyOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PharmacyOrderMapper extends BaseMapper<PharmacyOrder> {
    @Select("SELECT * FROM pharmacy_order WHERE id=#{id} FOR UPDATE")
    PharmacyOrder lockById(@Param("id") Long id);

    @Select("SELECT id FROM pharmacy_order WHERE order_status = 'PENDING_PAYMENT' " +
            "AND payment_deadline IS NOT NULL AND payment_deadline <= #{deadline} " +
            "ORDER BY payment_deadline ASC, id ASC LIMIT #{batchSize}")
    List<Long> findExpiredPendingPaymentIds(@Param("deadline") LocalDateTime deadline,
                                            @Param("batchSize") int batchSize);

    @Select("SELECT * FROM pharmacy_order WHERE user_id=#{userId} AND idempotency_key=#{key} LIMIT 1")
    PharmacyOrder findByIdempotencyKey(@Param("userId") Long userId, @Param("key") String key);
}
