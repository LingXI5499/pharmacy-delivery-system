package com.pharmacy.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pharmacy.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    @Select("SELECT * FROM sys_user WHERE id=#{id} FOR UPDATE")
    SysUser lockById(@Param("id") Long id);
    @Delete("DELETE FROM sys_user_role WHERE user_id=#{userId}") int clearRoles(@Param("userId") Long userId);
    @Insert("INSERT INTO sys_user_role(user_id,role_id) SELECT #{userId},id FROM sys_role WHERE role_code=#{role}") int addRole(@Param("userId") Long userId,@Param("role") String role);
    @Select("SELECT COUNT(*) FROM sys_user_role ur JOIN sys_role_permission rp ON rp.role_id=ur.role_id JOIN sys_permission p ON p.id=rp.permission_id WHERE ur.user_id=#{userId} AND p.permission_code=#{permission}") long countPermission(@Param("userId")Long userId,@Param("permission")String permission);
}
