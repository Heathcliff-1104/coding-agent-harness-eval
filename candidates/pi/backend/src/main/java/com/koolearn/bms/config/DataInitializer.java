package com.koolearn.bms.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.koolearn.bms.entity.BackupConfig;
import com.koolearn.bms.entity.Permission;
import com.koolearn.bms.entity.Role;
import com.koolearn.bms.entity.RolePermission;
import com.koolearn.bms.entity.User;
import com.koolearn.bms.mapper.BackupConfigMapper;
import com.koolearn.bms.mapper.PermissionMapper;
import com.koolearn.bms.mapper.RoleMapper;
import com.koolearn.bms.mapper.RolePermissionMapper;
import com.koolearn.bms.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 启动数据初始化（幂等）：
 * 1. 角色种子：admin / warehouse / engineer / purchaser / inspector / manager
 * 2. 权限种子：菜单（路由路径）+ 按钮（按钮码）
 * 3. 角色-权限映射种子
 * 4. 默认账号：admin / Admin@123456（管理员），warehouse / Warehouse@123456（库管员，仅当用户表为空）
 */
@Slf4j
@Component
public class DataInitializer implements ApplicationRunner {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final BackupConfigMapper backupConfigMapper;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserMapper userMapper, RoleMapper roleMapper,
                           PermissionMapper permissionMapper,
                           RolePermissionMapper rolePermissionMapper,
                           BackupConfigMapper backupConfigMapper,
                           PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.backupConfigMapper = backupConfigMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedRoles();
        seedPermissions();
        seedRolePermissions();
        seedUsers();
        seedBackupConfigs();
        log.info("DataInitializer 数据初始化完成");
    }

    // ============ 角色 ============
    private static final String[][] ROLES = {
            {"admin", "管理员", "all", "全部功能"},
            {"warehouse", "库管员", "all", "入库/出库/库存/报表"},
            {"engineer", "工程师", "self", "生产领料/物料检索/库存查询"},
            {"purchaser", "采购员", "all", "库存预警/补货/报表"},
            {"inspector", "质检员", "dept", "入库记录/库存查询"},
            {"manager", "部门主管", "dept", "除系统配置外的全部功能"},
    };

    private void seedRoles() {
        for (String[] r : ROLES) {
            Long cnt = roleMapper.selectCount(new LambdaQueryWrapper<Role>().eq(Role::getRoleCode, r[0]));
            if (cnt != null && cnt > 0) {
                continue;
            }
            Role role = new Role();
            role.setRoleCode(r[0]);
            role.setRoleName(r[1]);
            role.setDataScope(r[2]);
            role.setDescription(r[3]);
            roleMapper.insert(role);
        }
    }

    // ============ 权限 ============
    // 菜单：{permCode(路由路径), permName, parentCode}
    private static final String[][] MENUS = {
            {"/inbound/purchase", "采购入库", "inbound-mgmt"},
            {"/inbound/return", "退库入库", "inbound-mgmt"},
            {"/inbound/records", "入库记录", "inbound-mgmt"},
            {"/outbound/picking", "生产领料", "outbound-mgmt"},
            {"/outbound/records", "出库记录", "outbound-mgmt"},
            {"/inventory/search", "物料检索", "inventory-mgmt"},
            {"/inventory/query", "库存查询", "inventory-mgmt"},
            {"/inventory/alert", "库存预警", "inventory-mgmt"},
            {"/inventory/flow", "库存流水", "inventory-mgmt"},
            {"/report/inventory-detail", "库存明细", "report-mgmt"},
            {"/report/inbound-stats", "入库统计", "report-mgmt"},
            {"/report/outbound-stats", "出库统计", "report-mgmt"},
            {"/report/stagnant", "呆滞物品", "report-mgmt"},
            {"/report/export", "导出报表", "report-mgmt"},
            {"/system/users", "用户管理", "sys-mgmt"},
            {"/system/roles", "角色权限", "sys-mgmt"},
            {"/system/backup", "数据备份", "sys-mgmt"},
            {"/system/logs", "系统日志", "sys-mgmt"},
            {"/system/password", "密码修改", "sys-mgmt"},
    };

    // 按钮：{permCode, permName}
    private static final String[][] BUTTONS = {
            {"inbound:confirm", "入库确认"},
            {"inbound:batchConfirm", "批量审核入库"},
            {"outbound:confirm", "出库确认"},
            {"outbound:reject", "出库驳回"},
            {"alert:scan", "手动扫描预警"},
            {"alert:handle", "处理预警"},
            {"backup:db", "手动备份"},
            {"user:import", "用户导入"},
            {"user:export", "用户导出"},
            {"user:resetPwd", "重置密码"},
            {"log:export", "日志导出"},
            {"report:export", "报表导出"},
            {"replenishment:apply", "补货申请"},
            {"material:add", "新增物料"},
            {"material:update", "编辑物料"},
            {"material:del", "删除物料"},
            {"bom:import", "BOM导入"},
            {"bom:match", "BOM匹配"},
            {"bom:plan", "保存备料计划"},
            {"cis:sync", "CIS同步"},
    };

    private void seedPermissions() {
        int sort = 0;
        for (String[] m : MENUS) {
            insertPermissionIfAbsent(m[0], m[1], "menu", m[2], m[0], sort++);
        }
        for (String[] b : BUTTONS) {
            insertPermissionIfAbsent(b[0], b[1], "button", null, null, sort++);
        }
    }

    private void insertPermissionIfAbsent(String code, String name, String type, String parent, String path, int sort) {
        Long cnt = permissionMapper.selectCount(new LambdaQueryWrapper<Permission>().eq(Permission::getPermCode, code));
        if (cnt != null && cnt > 0) {
            return;
        }
        Permission p = new Permission();
        p.setPermCode(code);
        p.setPermName(name);
        p.setPermType(type);
        p.setParentCode(parent);
        p.setPath(path);
        p.setSortNo(sort);
        permissionMapper.insert(p);
    }

    // ============ 角色-权限映射 ============
    private static final String[] SYSTEM_MENUS = {"/system/users", "/system/roles", "/system/backup", "/system/logs"};

    // 报表统计菜单/按钮：StatisticsController 仅 admin/warehouse 可访问，其他角色不应持有
    private static final String[] REPORT_MENUS = {
            "/report/inventory-detail", "/report/inbound-stats", "/report/outbound-stats",
            "/report/stagnant", "/report/export", "report:export"};

    private void seedRolePermissions() {
        seedRolePermission("admin", null); // 全部
        seedRolePermission("warehouse", buildExcludeSet(SYSTEM_MENUS)); // 除系统管理菜单
        seedRolePermission("manager", buildExcludeSet(concat(SYSTEM_MENUS, REPORT_MENUS))); // 除系统管理+报表（后端仅 admin/warehouse）
        seedRolePermission("engineer", new HashSet<>(Arrays.asList(
                "/outbound/picking", "/inventory/search", "/inventory/query", "/system/password",
                "bom:import", "bom:match", "bom:plan")));
        seedRolePermission("purchaser", new HashSet<>(Arrays.asList(
                "/inbound/records", "/inventory/search", "/inventory/query",
                "/system/password", "replenishment:apply")));
        seedRolePermission("inspector", new HashSet<>(Arrays.asList(
                "/inbound/records", "/inventory/search", "/inventory/query",
                "/system/password")));
    }

    private String[] concat(String[] a, String[] b) {
        String[] r = new String[a.length + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }

    private Set<String> buildExcludeSet(String[] excludes) {
        Set<String> set = new HashSet<>();
        for (String[] m : MENUS) {
            set.add(m[0]);
        }
        for (String[] b : BUTTONS) {
            set.add(b[0]);
        }
        set.removeAll(Arrays.asList(excludes));
        return set;
    }

    private void seedRolePermission(String roleCode, Set<String> includeCodes) {
        Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getRoleCode, roleCode));
        if (role == null) {
            return;
        }
        Long cnt = rolePermissionMapper.selectCount(new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, role.getId()));
        if (cnt != null && cnt > 0) {
            return; // 已有映射，不覆盖管理员配置
        }
        List<Permission> perms;
        if (includeCodes == null) {
            perms = permissionMapper.selectList(null);
        } else {
            perms = permissionMapper.selectList(new LambdaQueryWrapper<Permission>().in(Permission::getPermCode, includeCodes));
        }
        for (Permission p : perms) {
            RolePermission rp = new RolePermission();
            rp.setRoleId(role.getId());
            rp.setPermissionId(p.getId());
            rolePermissionMapper.insert(rp);
        }
    }

    // ============ 默认备份配置 ============
    private void seedBackupConfigs() {
        seedBackupConfig("full", "0 0 2 ? * SUN");
        seedBackupConfig("incremental", "0 0 3 * * ?");
    }

    private void seedBackupConfig(String type, String cron) {
        Long cnt = backupConfigMapper.selectCount(new LambdaQueryWrapper<BackupConfig>()
                .eq(BackupConfig::getBackupType, type));
        if (cnt != null && cnt > 0) {
            return;
        }
        BackupConfig cfg = new BackupConfig();
        cfg.setBackupType(type);
        cfg.setCronExpr(cron);
        cfg.setRetentionDays(30);
        cfg.setEnabled(1);
        backupConfigMapper.insert(cfg);
    }

    // ============ 默认账号 ============
    private void seedUsers() {
        Long cnt = userMapper.selectCount(null);
        if (cnt != null && cnt > 0) {
            return;
        }
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("Admin@123456"));
        admin.setRealName("系统管理员");
        admin.setDept("信息部");
        admin.setRole("admin");
        admin.setStatus(1);
        userMapper.insert(admin);
        log.info("已创建默认管理员账号 admin");

        User warehouse = new User();
        warehouse.setUsername("warehouse");
        warehouse.setPassword(passwordEncoder.encode("Warehouse@123456"));
        warehouse.setRealName("库管员");
        warehouse.setDept("仓储部");
        warehouse.setRole("warehouse");
        warehouse.setStatus(1);
        userMapper.insert(warehouse);
        log.info("已创建默认库管员账号 warehouse");
    }
}
