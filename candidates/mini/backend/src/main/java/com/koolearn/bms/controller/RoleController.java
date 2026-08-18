package com.koolearn.bms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.koolearn.bms.annotation.RequireRole;
import com.koolearn.bms.entity.SysRole;
import com.koolearn.bms.entity.SysRoleMenu;
import com.koolearn.bms.mapper.SysRoleMapper;
import com.koolearn.bms.mapper.SysRoleMenuMapper;
import com.koolearn.bms.util.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequireRole("admin")
@RestController
@RequestMapping("/role")
public class RoleController {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    public RoleController(SysRoleMapper roleMapper, SysRoleMenuMapper roleMenuMapper) {
        this.roleMapper = roleMapper;
        this.roleMenuMapper = roleMenuMapper;
    }

    @GetMapping("/list")
    public Result<List<SysRole>> list() {
        return Result.success(roleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId)));
    }

    @GetMapping("/menus")
    public Result<List<String>> menus(@RequestParam String roleCode) {
        List<SysRoleMenu> list = roleMenuMapper.selectList(
                new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleCode, roleCode));
        return Result.success(list.stream().map(SysRoleMenu::getMenuPath).collect(Collectors.toList()));
    }

    @PostMapping("/save")
    public Result<String> saveRole(@RequestBody Map<String, Object> body) {
        String roleCode = body.get("roleCode") == null ? null : body.get("roleCode").toString();
        String roleName = body.get("roleName") == null ? null : body.get("roleName").toString();
        String description = body.get("description") == null ? null : body.get("description").toString();
        if (roleCode == null || roleCode.trim().isEmpty()) return Result.fail("角色编码不能为空");
        if (roleName == null || roleName.trim().isEmpty()) return Result.fail("角色名称不能为空");

        SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, roleCode));
        if (role == null) {
            role = new SysRole();
            role.setRoleCode(roleCode);
            role.setRoleName(roleName);
            role.setDescription(description);
            roleMapper.insert(role);
        } else {
            role.setRoleName(roleName);
            role.setDescription(description);
            roleMapper.updateById(role);
        }

        // 保存菜单权限
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleCode, roleCode));
        Object menusObj = body.get("menus");
        if (menusObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> menus = (List<String>) menusObj;
            for (String path : menus) {
                SysRoleMenu rm = new SysRoleMenu();
                rm.setRoleCode(roleCode);
                rm.setMenuPath(path);
                rm.setDataScope("all");
                roleMenuMapper.insert(rm);
            }
        }
        return Result.success("角色权限已保存");
    }
}
