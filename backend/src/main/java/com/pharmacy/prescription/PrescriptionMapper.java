package com.pharmacy.prescription;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PrescriptionMapper extends BaseMapper<Prescription> {
    @Select("SELECT * FROM prescription WHERE id=#{id} FOR UPDATE") Prescription lockById(@Param("id") Long id);
}
