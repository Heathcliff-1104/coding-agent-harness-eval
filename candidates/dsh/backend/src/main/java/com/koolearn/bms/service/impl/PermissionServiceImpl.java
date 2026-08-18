package com.koolearn.bms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.koolearn.bms.entity.*;
import com.koolearn.bms.mapper.*;
import com.koolearn.bms.service.PermissionService;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PermissionServiceImpl implements PermissionService {

    private final UserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysPermissionMapper permissionMapper;

    public PermissionServiceImpl(UserMapper userMapper, SysRoleMapper roleMapper,
                                 SysUserRoleMapper userRoleMapper,
                                 SysRolePermissionMapper rolePermissionMapper,
                                 SysPermissionMapper permissionMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.permissionMapper = permissionMapper;
    }

    @Override
    public Set<String> loadPermissionCodes(Long userId) {
        Set<Long> roleIds = new HashSet<>();
        User user = userMapper.selectById(userId);
        if (user != null && user.getRole() != null) {
            SysRole primary = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, user.getRole()));
            if (primary != null) roleIds.add(primary.getId());
        }
        List<SysUserRole> extras = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        extras.forEach(ur -> roleIds.add(ur.getRoleId()));

        if (roleIds.isEmpty()) return new HashSet<>();
        List<SysRolePermission> rps = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<SysRolePermission>().in(SysRolePermission::getRoleId, roleIds));
        if (rps.isEmpty()) return new HashSet<>();
        Set<Long> permIds = rps.stream().map(SysRolePermission::getPermissionId).collect(Collectors.toSet());
        List<SysPermission> perms = permissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>().in(SysPermission::getId, permIds));
        return perms.stream().map(SysPermission::getCode).collect(Collectors.toSet());
    }

    @Override
    public String loadDataScope(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getRole() == null) return "self";
        SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, user.getRole()));
        return role == null || role.getDataScope() == null ? "self" : role.getDataScope();
    }
}
