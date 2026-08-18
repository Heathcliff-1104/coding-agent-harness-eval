package com.koolearn.bms.service;

import java.util.Set;

/**
 * 权限服务：每次请求从数据库重新加载用户角色与权限，保证权限变更实时生效。
 */
public interface PermissionService {

    /** 加载用户有效权限码集合（主角色 + 附加角色对应的所有权限）。 */
    Set<String> loadPermissionCodes(Long userId);

    /** 查询用户主角色对应的数据范围（self/dept/all）。 */
    String loadDataScope(Long userId);
}
