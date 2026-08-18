package com.koolearn.bms.controller;

import com.koolearn.bms.annotation.RequireRole;
import com.koolearn.bms.config.RoleInterceptor;
import com.koolearn.bms.entity.Role;
import com.koolearn.bms.service.RoleService;
import com.koolearn.bms.service.SysOperationLogService;
import com.koolearn.bms.util.Result;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RequireRole("admin")
@RestController
@RequestMapping("/role")
public class RoleController {

    private final RoleService roleService;
    private final SysOperationLogService sysLogService;
    private final RoleInterceptor roleInterceptor;

    public RoleController(RoleService roleService, SysOperationLogService sysLogService, RoleInterceptor roleInterceptor) {
        this.roleService = roleService;
        this.sysLogService = sysLogService;
        this.roleInterceptor = roleInterceptor;
    }

    @GetMapping("/list")
    public Result<List<Role>> list() {
        return Result.success(roleService.list());
    }

    @GetMapping("/permission-tree")
    public Result<List<Map<String, Object>>> permissionTree() {
        return Result.success(roleService.permissionTree());
    }

    @GetMapping("/{id}/permissions")
    public Result<List<Long>> rolePermissions(@PathVariable Long id) {
        return Result.success(roleService.getRolePermissionIds(id));
    }

    @PutMapping("/{id}/permissions")
    public Result<String> savePermissions(@PathVariable Long id,
                                          @RequestBody Map<String, Object> body,
                                          @RequestAttribute("username") String operator,
                                          HttpServletRequest request) {
        @SuppressWarnings("unchecked")
        List<Object> rawIds = (List<Object>) body.get("permissionIds");
        List<Long> permissionIds = new java.util.ArrayList<>();
        if (rawIds != null) {
            for (Object o : rawIds) {
                permissionIds.add(Long.valueOf(o.toString()));
            }
        }
        String dataScope = body.get("dataScope") != null ? body.get("dataScope").toString() : null;
        roleService.saveRolePermissions(id, permissionIds, dataScope);
        // 权限变更立即生效：该角色下所有用户的缓存全部失效（30s 缓存不再等待）
        roleInterceptor.evictAll();
        sysLogService.log(operator, "修改权限", "修改角色权限: 角色ID=" + id, getIp(request));
        return Result.success("保存成功");
    }

    @PostMapping
    public Result<String> create(@RequestBody Role role,
                                 @RequestAttribute("username") String operator,
                                 HttpServletRequest request) {
        role.setId(null);
        if (role.getRoleCode() == null || role.getRoleCode().trim().isEmpty()) {
            return Result.fail("角色编码不能为空");
        }
        roleService.save(role);
        sysLogService.log(operator, "新增角色", "新增角色: " + role.getRoleName(), getIp(request));
        return Result.success("新增成功");
    }

    @PutMapping("/{id}")
    public Result<String> update(@PathVariable Long id, @RequestBody Role role,
                                 @RequestAttribute("username") String operator,
                                 HttpServletRequest request) {
        role.setId(id);
        roleService.updateById(role);
        // 角色信息（如角色名/数据范围）变更：清除全部用户缓存
        roleInterceptor.evictAll();
        sysLogService.log(operator, "修改角色", "修改角色: " + role.getRoleName(), getIp(request));
        return Result.success("更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id,
                                 @RequestAttribute("username") String operator,
                                 HttpServletRequest request) {
        Role role = roleService.getById(id);
        roleService.deleteRole(id);
        roleInterceptor.evictAll();
        sysLogService.log(operator, "删除角色", "删除角色: " + (role != null ? role.getRoleName() : id), getIp(request));
        return Result.success("删除成功");
    }

    private String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isEmpty()) ip = request.getRemoteAddr();
        return ip;
    }
}
