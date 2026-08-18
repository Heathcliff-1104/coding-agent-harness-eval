package com.koolearn.bms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.koolearn.bms.annotation.RequireRole;
import com.koolearn.bms.entity.SysPermission;
import com.koolearn.bms.entity.SysRole;
import com.koolearn.bms.entity.SysRolePermission;
import com.koolearn.bms.mapper.SysPermissionMapper;
import com.koolearn.bms.mapper.SysRoleMapper;
import com.koolearn.bms.mapper.SysRolePermissionMapper;
import com.koolearn.bms.service.SysOperationLogService;
import com.koolearn.bms.util.Result;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 角色权限管理（需求 2.6.2）：自定义角色 + 菜单/按钮权限勾选 + 数据范围。
 * 权限变更写入数据库，LoginInterceptor 每次请求从数据库重新加载，实时生效。
 */
@RequireRole("admin")
@RestController
@RequestMapping("/role")
public class RoleController {

    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysOperationLogService sysLogService;

    public RoleController(SysRoleMapper roleMapper, SysPermissionMapper permissionMapper,
                          SysRolePermissionMapper rolePermissionMapper,
                          SysOperationLogService sysLogService) {
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.sysLogService = sysLogService;
    }

    @GetMapping("/list")
    public Result<List<SysRole>> list() {
        return Result.success(roleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId)));
    }

    @GetMapping("/page")
    public Result<IPage<SysRole>> page(@RequestParam(defaultValue = "1") Long pageNum,
                                       @RequestParam(defaultValue = "10") Long pageSize,
                                       @RequestParam(required = false) String keyword) {
        Page<SysRole> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysRole> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(SysRole::getName, keyword).or().like(SysRole::getCode, keyword));
        }
        qw.orderByAsc(SysRole::getId);
        return Result.success(roleMapper.selectPage(page, qw));
    }

    /** 全部权限清单（菜单+按钮） */
    @GetMapping("/permissionList")
    public Result<List<SysPermission>> permissionList() {
        return Result.success(permissionMapper.selectList(new LambdaQueryWrapper<SysPermission>().orderByAsc(SysPermission::getSort)));
    }

    /** 某角色已勾选的权限码 */
    @GetMapping("/permissions/{roleCode}")
    public Result<List<String>> rolePermissions(@PathVariable String roleCode) {
        SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, roleCode));
        if (role == null) return Result.success(Collections.emptyList());
        List<SysRolePermission> rps = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, role.getId()));
        if (rps.isEmpty()) return Result.success(Collections.emptyList());
        Set<Long> permIds = rps.stream().map(SysRolePermission::getPermissionId).collect(Collectors.toSet());
        List<SysPermission> perms = permissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>().in(SysPermission::getId, permIds));
        return Result.success(perms.stream().map(SysPermission::getCode).collect(Collectors.toList()));
    }

    @PostMapping("/save")
    public Result<String> save(@RequestBody SysRole role, @RequestAttribute("username") String operator,
                               HttpServletRequest request) {
        if (!StringUtils.hasText(role.getCode()) || !StringUtils.hasText(role.getName())) {
            return Result.fail("角色编码与名称不能为空");
        }
        SysRole exist = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, role.getCode()));
        if (exist != null && (role.getId() == null || !exist.getId().equals(role.getId()))) {
            return Result.fail("角色编码已存在");
        }
        if (role.getDataScope() == null) role.setDataScope("all");
        if (role.getId() == null) {
            roleMapper.insert(role);
        } else {
            roleMapper.updateById(role);
        }
        sysLogService.log(operator, "修改权限", "保存角色: " + role.getCode(), getIp(request));
        return Result.success("保存成功");
    }

    /** 保存角色权限勾选：body = {roleCode, permissionCodes: [...]} */
    @PostMapping("/updatePermissions")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> updatePermissions(@RequestBody Map<String, Object> body,
                                            @RequestAttribute("username") String operator,
                                            HttpServletRequest request) {
        String roleCode = (String) body.get("roleCode");
        @SuppressWarnings("unchecked")
        List<String> codes = (List<String>) body.get("permissionCodes");
        if (!StringUtils.hasText(roleCode) || codes == null) {
            return Result.fail("参数不完整");
        }
        SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, roleCode));
        if (role == null) return Result.fail("角色不存在");
        rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, role.getId()));
        if (!codes.isEmpty()) {
            List<SysPermission> perms = permissionMapper.selectList(
                    new LambdaQueryWrapper<SysPermission>().in(SysPermission::getCode, codes));
            for (SysPermission perm : perms) {
                SysRolePermission rp = new SysRolePermission();
                rp.setRoleId(role.getId());
                rp.setPermissionId(perm.getId());
                rolePermissionMapper.insert(rp);
            }
        }
        sysLogService.log(operator, "修改权限", "更新角色[" + roleCode + "]权限: " + codes.size() + "项（实时生效）", getIp(request));
        return Result.success("权限配置已保存（实时生效）");
    }

    @DeleteMapping("/delete/{id}")
    public Result<String> delete(@PathVariable Long id, @RequestAttribute("username") String operator,
                                 HttpServletRequest request) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) return Result.fail("角色不存在");
        if ("admin".equals(role.getCode())) {
            return Result.fail("内置管理员角色不可删除");
        }
        roleMapper.deleteById(id);
        rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, id));
        sysLogService.log(operator, "修改权限", "删除角色: " + role.getCode(), getIp(request));
        return Result.success("删除成功");
    }

    private String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isEmpty()) ip = request.getRemoteAddr();
        return ip;
    }
}
