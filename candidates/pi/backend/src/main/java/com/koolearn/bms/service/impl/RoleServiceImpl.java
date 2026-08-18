package com.koolearn.bms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.koolearn.bms.entity.Permission;
import com.koolearn.bms.entity.Role;
import com.koolearn.bms.entity.RolePermission;
import com.koolearn.bms.mapper.PermissionMapper;
import com.koolearn.bms.mapper.RoleMapper;
import com.koolearn.bms.mapper.RolePermissionMapper;
import com.koolearn.bms.mapper.UserMapper;
import com.koolearn.bms.service.RoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    private final RolePermissionMapper rolePermissionMapper;
    private final PermissionMapper permissionMapper;
    private final UserMapper userMapper;

    public RoleServiceImpl(RolePermissionMapper rolePermissionMapper,
                           PermissionMapper permissionMapper,
                           UserMapper userMapper) {
        this.rolePermissionMapper = rolePermissionMapper;
        this.permissionMapper = permissionMapper;
        this.userMapper = userMapper;
    }

    @Override
    public List<Map<String, Object>> permissionTree() {
        List<Permission> all = permissionMapper.selectList(new LambdaQueryWrapper<Permission>()
                .orderByAsc(Permission::getSortNo));
        // 按 parent 分组
        Map<String, List<Permission>> groups = all.stream()
                .filter(p -> p.getParentCode() != null)
                .collect(Collectors.groupingBy(Permission::getParentCode, LinkedHashMap::new, Collectors.toList()));

        List<Map<String, Object>> roots = new ArrayList<>();
        // 顶层分组：每个 parent 一个节点
        for (Map.Entry<String, List<Permission>> e : groups.entrySet()) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("label", groupLabel(e.getKey()));
            node.put("path", e.getKey());
            List<Map<String, Object>> children = new ArrayList<>();
            for (Permission p : e.getValue()) {
                Map<String, Object> child = new LinkedHashMap<>();
                child.put("id", p.getId());
                child.put("label", p.getPermName());
                child.put("code", p.getPermCode());
                child.put("path", p.getPath() != null ? p.getPath() : p.getPermCode());
                child.put("type", p.getPermType());
                children.add(child);
            }
            node.put("children", children);
            roots.add(node);
        }
        return roots;
    }

    private String groupLabel(String parent) {
        switch (parent == null ? "" : parent) {
            case "inbound-mgmt": return "入库管理";
            case "outbound-mgmt": return "出库管理";
            case "inventory-mgmt": return "库存管理";
            case "report-mgmt": return "报表统计";
            case "sys-mgmt": return "系统管理";
            default: return parent;
        }
    }

    @Override
    public List<Long> getRolePermissionIds(Long roleId) {
        return rolePermissionMapper.selectList(new LambdaQueryWrapper<RolePermission>()
                        .eq(RolePermission::getRoleId, roleId))
                .stream().map(RolePermission::getPermissionId).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRolePermissions(Long roleId, List<Long> permissionIds, String dataScope) {
        Role role = getById(roleId);
        if (role == null) throw new RuntimeException("角色不存在");
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>()
                .eq(RolePermission::getRoleId, roleId));
        if (!CollectionUtils.isEmpty(permissionIds)) {
            for (Long pid : permissionIds) {
                RolePermission rp = new RolePermission();
                rp.setRoleId(roleId);
                rp.setPermissionId(pid);
                rolePermissionMapper.insert(rp);
            }
        }
        if (dataScope != null && !dataScope.isEmpty()) {
            role.setDataScope(dataScope);
            updateById(role);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long roleId) {
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>()
                .eq(RolePermission::getRoleId, roleId));
        removeById(roleId);
    }

    @Override
    public List<String> getPermissionsByUserId(Long userId) {
        return userMapper.selectPermissionsByUserId(userId).stream()
                .map(Permission::getPermCode).collect(Collectors.toList());
    }

    @Override
    public String getDataScopeByUserId(Long userId) {
        String scope = userMapper.selectDataScopeByUserId(userId);
        return scope != null ? scope : "self";
    }

    @Override
    public List<Permission> getPermissionListByUserId(Long userId) {
        return userMapper.selectPermissionsByUserId(userId);
    }
}
