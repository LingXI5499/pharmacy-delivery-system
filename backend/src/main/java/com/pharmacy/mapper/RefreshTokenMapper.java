package com.pharmacy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pharmacy.entity.RefreshToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface RefreshTokenMapper extends BaseMapper<RefreshToken> {
    @Select("SELECT * FROM refresh_token WHERE token_hash = #{tokenHash} FOR UPDATE")
    RefreshToken selectForUpdate(@Param("tokenHash") String tokenHash);

    @Update("UPDATE refresh_token SET revoked_at = #{now} WHERE family_id = #{familyId} AND revoked_at IS NULL")
    int revokeFamily(@Param("familyId") String familyId, @Param("now") LocalDateTime now);
}
