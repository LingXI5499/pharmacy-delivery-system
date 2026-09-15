package com.pharmacy.payment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;import org.apache.ibatis.annotations.*;
@Mapper public interface RefundRecordMapper extends BaseMapper<RefundRecord>{@Select("SELECT * FROM refund_record WHERE refund_no=#{no} FOR UPDATE")RefundRecord lockByNo(@Param("no")String no);}
