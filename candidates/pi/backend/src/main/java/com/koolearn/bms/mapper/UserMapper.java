package com.koolearn.bms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.koolearn.bms.entity.Permission;
import com.koolearn.bms.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    /** 通过用户ID查询其角色拥有的全部权限（忽略 JWT 角色声明，实时从 DB 读取） */
    @Select("SELECT DISTINCT p.* FROM sys_permission p "
            + "JOIN sys_role_permission rp ON p.id = rp.permission_id "
            + "JOIN sys_role r ON r.id = rp.role_id "
            + "JOIN sys_user u ON u.role = r.role_code "
            + "WHERE u.id = #{userId}")
    List<Permission> selectPermissionsByUserId(@Param("userId") Long userId);

    /** 通过用户ID查询角色数据范围 (all/dept/self) */
    @Select("SELECT r.data_scope FROM sys_role r JOIN sys_user u ON u.role = r.role_code WHERE u.id = #{userId}")
    String selectDataScopeByUserId(@Param("userId") Long userId);
}
