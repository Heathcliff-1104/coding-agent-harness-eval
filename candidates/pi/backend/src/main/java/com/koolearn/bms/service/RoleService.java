package com.koolearn.bms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.koolearn.bms.entity.Permission;
import com.koolearn.bms.entity.Role;

import java.util.List;
import java.util.Map;

public interface RoleService extends IService<Role> {

    /** 权限树（按 parent 分组嵌套） */
    List<Map<String, Object>> permissionTree();

    /** 某角色已拥有的权限ID列表 */
    List<Long> getRolePermissionIds(Long roleId);

    /** 覆盖式保存角色权限 + 数据范围 */
    void saveRolePermissions(Long roleId, List<Long> permissionIds, String dataScope);

    /** 删除角色（同时清理角色-权限映射） */
    void deleteRole(Long roleId);

    /** 某用户拥有的权限编码列表 */
    List<String> getPermissionsByUserId(Long userId);

    /** 某用户的数据范围 */
    String getDataScopeByUserId(Long userId);

    /** 某用户拥有的权限实体列表 */
    List<Permission> getPermissionListByUserId(Long userId);
}
