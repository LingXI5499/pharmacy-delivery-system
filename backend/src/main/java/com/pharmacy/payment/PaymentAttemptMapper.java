package com.pharmacy.payment;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper public interface PaymentAttemptMapper extends BaseMapper<PaymentAttempt> {
    @Select("SELECT * FROM payment_attempt WHERE payment_no=#{paymentNo} FOR UPDATE") PaymentAttempt lockByNo(@Param("paymentNo") String paymentNo);
}
