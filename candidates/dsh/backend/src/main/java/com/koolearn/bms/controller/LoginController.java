package com.koolearn.bms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.koolearn.bms.annotation.RequireRole;
import com.koolearn.bms.config.LoginInterceptor;
import com.koolearn.bms.config.LoginRateLimiter;
import com.koolearn.bms.entity.InboundOrder;
import com.koolearn.bms.entity.OutboundOrder;
import com.koolearn.bms.entity.User;
import com.koolearn.bms.mapper.InboundOrderMapper;
import com.koolearn.bms.mapper.OutboundOrderMapper;
import com.koolearn.bms.service.DingTalkLoginService;
import com.koolearn.bms.service.LoginLogService;
import com.koolearn.bms.service.PermissionService;
import com.koolearn.bms.service.SysOperationLogService;
import com.koolearn.bms.service.UserService;
import com.koolearn.bms.util.CaptchaUtil;
import com.koolearn.bms.util.JwtUtil;
import com.koolearn.bms.util.PasswordPolicy;
import com.koolearn.bms.util.Result;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/user")
public class LoginController {

    private final UserService userService;
    private final LoginLogService loginLogService;
    private final SysOperationLogService sysLogService;
    private final PasswordEncoder passwordEncoder;
    private final LoginRateLimiter rateLimiter;
    private final DingTalkLoginService dingTalkLoginService;
    private final PermissionService permissionService;
    private final InboundOrderMapper inboundOrderMapper;
    private final OutboundOrderMapper outboundOrderMapper;

    public LoginController(UserService userService, LoginLogService loginLogService,
                           SysOperationLogService sysLogService, PasswordEncoder passwordEncoder,
                           LoginRateLimiter rateLimiter, DingTalkLoginService dingTalkLoginService,
                           PermissionService permissionService,
                           InboundOrderMapper inboundOrderMapper,
                           OutboundOrderMapper outboundOrderMapper) {
        this.userService = userService;
        this.loginLogService = loginLogService;
        this.sysLogService = sysLogService;
        this.passwordEncoder = passwordEncoder;
        this.rateLimiter = rateLimiter;
        this.dingTalkLoginService = dingTalkLoginService;
        this.permissionService = permissionService;
        this.inboundOrderMapper = inboundOrderMapper;
        this.outboundOrderMapper = outboundOrderMapper;
    }

    /** 实时校验用户名/手机号唯一性（需求 2.1.1 注册时实时校验） */
    @GetMapping("/check")
    public Result<Map<String, Boolean>> check(@RequestParam(required = false) String username,
                                              @RequestParam(required = false) String phone) {
        Map<String, Boolean> data = new HashMap<>();
        data.put("usernameTaken", false);
        data.put("phoneTaken", false);
        if (username != null && !username.isEmpty()) {
            data.put("usernameTaken", userService.count(new LambdaQueryWrapper<User>().eq(User::getUsername, username.trim())) > 0);
        }
        if (phone != null && !phone.isEmpty()) {
            data.put("phoneTaken", userService.count(new LambdaQueryWrapper<User>().eq(User::getPhone, phone.trim())) > 0);
        }
        return Result.success(data);
    }

    @GetMapping("/captcha")
    public Result<Map<String, String>> captcha() {
        CaptchaUtil.CaptchaResult cr = CaptchaUtil.generate();
        Map<String, String> data = new HashMap<>();
        data.put("captchaKey", cr.key);
        data.put("captchaImage", cr.image);
        return Result.success(data);
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String username = body.get("username");
        String password = body.get("password");
        String captchaKey = body.get("captchaKey");
        String captchaCode = body.get("captchaCode");
        // 限流键 = IP + 用户名，防一人锁死全站
        String rateKey = getIp(request) + "|" + (username == null ? "" : username);
        if (rateLimiter.isBlocked(rateKey)) {
            return Result.fail(429, "登录过于频繁，请5分钟后再试");
        }
        if (!CaptchaUtil.verify(captchaKey, captchaCode)) {
            // 验证码错误不累计锁定（防止攻击者用错误验证码锁死他人账号）
            loginLogService.record(username, getIp(request), getDevice(request), 0);
            return Result.fail("验证码错误");
        }
        try {
            User user = userService.login(username, password);
            String token = JwtUtil.generate(user.getId(), user.getUsername(), user.getRole());
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("username", user.getUsername());
            data.put("realName", user.getRealName());
            data.put("role", user.getRole());
            loginLogService.record(username, getIp(request), getDevice(request), 1);
            // 记录上次登录时间
            User update = new User();
            update.setId(user.getId());
            update.setLastLoginTime(LocalDateTime.now());
            userService.updateById(update);
            return Result.success(data);
        } catch (RuntimeException e) {
            // 仅密码校验失败累计锁定次数
            rateLimiter.recordAttempt(rateKey);
            loginLogService.record(username, getIp(request), getDevice(request), 0);
            throw e;
        }
    }

    @PostMapping("/register")
    public Result<String> register(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String captchaKey = body.get("captchaKey");
        String captchaCode = body.get("captchaCode");
        if (!CaptchaUtil.verify(captchaKey, captchaCode)) {
            return Result.fail("验证码错误或已过期");
        }
        User user = new User();
        user.setUsername(body.get("username"));
        user.setPassword(body.get("password"));
        user.setRealName(body.get("realName"));
        user.setPhone(body.get("phone"));
        user.setDept(body.get("dept"));
        userService.register(user);
        sysLogService.log(user.getUsername(), "用户注册", "新用户注册", getIp(request));
        return Result.success("注册成功");
    }

    @GetMapping("/info")
    public Result<Map<String, Object>> info(@RequestAttribute(value = "userId", required = false) Long userId) {
        if (userId == null) return Result.fail("未登录");
        User user = userService.getById(userId);
        if (user == null) return Result.fail("用户不存在");
        user.setPassword(null);
        Map<String, Object> data = new HashMap<>();
        data.put("user", user);
        data.put("permissions", permissionService.loadPermissionCodes(userId));
        data.put("dataScope", permissionService.loadDataScope(userId));
        return Result.success(data);
    }

    @PostMapping("/logout")
    public Result<String> logout(@RequestAttribute("username") String username, HttpServletRequest request) {
        sysLogService.log(username, "退出登录", "用户退出登录", getIp(request));
        return Result.success("退出成功");
    }

    @PutMapping("/changePwd")
    public Result<String> changePwd(@RequestAttribute("userId") Long userId,
                                     @RequestBody Map<String, String> body,
                                     HttpServletRequest request) {
        User user = userService.getById(userId);
        if (user == null) return Result.fail("用户不存在");
        if (body.get("oldPassword") == null || !passwordEncoder.matches(body.get("oldPassword"), user.getPassword())) {
            return Result.fail("原密码错误");
        }
        String newPwd = body.get("newPassword");
        String pwdErr = PasswordPolicy.validate(newPwd);
        if (pwdErr != null) return Result.fail(pwdErr);
        user.setPassword(passwordEncoder.encode(newPwd));
        userService.updateById(user);
        sysLogService.log(user.getUsername(), "修改密码", "用户修改密码", getIp(request));
        return Result.success("修改成功");
    }

    @RequireRole("admin")
    @GetMapping("/export")
    public void export(HttpServletResponse response) throws Exception {
        List<User> users = userService.list(new LambdaQueryWrapper<User>().orderByDesc(User::getCreateTime));
        List<java.util.Map<String, Object>> rows = new ArrayList<>();
        for (User u : users) {
            java.util.Map<String, Object> m = new HashMap<>();
            m.put("username", u.getUsername());
            m.put("realName", u.getRealName());
            m.put("dept", u.getDept());
            m.put("phone", u.getPhone());
            m.put("role", u.getRole());
            m.put("status", u.getStatus() != null && u.getStatus() == 1 ? "启用" : "禁用");
            m.put("lastLoginTime", u.getLastLoginTime());
            m.put("createTime", u.getCreateTime());
            rows.add(m);
        }
        com.koolearn.bms.util.ExcelExportUtil.export(response, "用户列表",
                new String[]{"用户名", "真实姓名", "部门", "手机号", "角色", "状态", "上次登录时间", "创建时间"},
                rows, new String[]{"username", "realName", "dept", "phone", "role", "status", "lastLoginTime", "createTime"});
    }

    @RequireRole("admin")
    @GetMapping("/list")
    public Result<List<User>> list() {
        List<User> users = userService.list(new LambdaQueryWrapper<User>().orderByDesc(User::getCreateTime));
        users.forEach(u -> u.setPassword(null));
        return Result.success(users);
    }

    @RequireRole("admin")
    @GetMapping("/page")
    public Result<IPage<User>> page(@RequestParam(defaultValue = "1") Long pageNum,
                                     @RequestParam(defaultValue = "10") Long pageSize,
                                     @RequestParam(required = false) String keyword) {
        Page<User> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            qw.and(w -> w.like(User::getUsername, keyword).or().like(User::getRealName, keyword));
        }
        qw.orderByDesc(User::getCreateTime);
        IPage<User> result = userService.page(page, qw);
        result.getRecords().forEach(u -> u.setPassword(null));
        return Result.success(result);
    }

    /** 管理员新增用户（可指定角色/状态/初始密码），不走公开注册接口。 */
    @RequireRole("admin")
    @PostMapping("/add")
    public Result<String> add(@RequestBody Map<String, String> body, @RequestAttribute("username") String operator,
                              HttpServletRequest request) {
        User user = new User();
        user.setUsername(body.get("username"));
        user.setPassword(body.get("password"));
        user.setRealName(body.get("realName"));
        user.setPhone(body.get("phone"));
        user.setDept(body.get("dept"));
        user.setRole(body.getOrDefault("role", "engineer"));
        user.setStatus("0".equals(body.get("status")) ? 0 : 1);
        userService.registerByAdmin(user);
        sysLogService.log(operator, "新增用户", "新增用户: " + user.getUsername() + " 角色: " + user.getRole(), getIp(request));
        return Result.success("新增成功");
    }

    /** 批量导入用户（Excel解析后的JSON数组），需求 2.6.1 */
    @RequireRole("admin")
    @PostMapping("/import")
    public Result<String> importUsers(@RequestBody List<Map<String, String>> users,
                                      @RequestAttribute("username") String operator,
                                      HttpServletRequest request) {
        if (users == null || users.isEmpty()) return Result.fail("导入数据为空");
        int ok = 0;
        StringBuilder errors = new StringBuilder();
        for (Map<String, String> u : users) {
            try {
                User user = new User();
                user.setUsername(u.get("username"));
                user.setPassword(u.get("password"));
                user.setRealName(u.get("realName"));
                user.setPhone(u.get("phone"));
                user.setDept(u.get("dept"));
                user.setRole(u.getOrDefault("role", "engineer"));
                user.setStatus("0".equals(u.get("status")) ? 0 : 1);
                userService.registerByAdmin(user);
                ok++;
            } catch (Exception e) {
                if (errors.length() > 0) errors.append("; ");
                errors.append(u.get("username")).append(": ").append(e.getMessage());
            }
        }
        sysLogService.log(operator, "批量导入用户", "导入成功 " + ok + " 条" + (errors.length() > 0 ? "，失败: " + errors : ""), getIp(request));
        return Result.success("导入完成，成功 " + ok + " 条" + (errors.length() > 0 ? "，失败: " + errors : ""));
    }

    @RequireRole("admin")
    @PutMapping("/update")
    public Result<String> update(@RequestBody User user, @RequestAttribute("username") String operator,
                                  HttpServletRequest request) {
        if (user.getId() == null) return Result.fail("用户ID不能为空");
        User exist = userService.getById(user.getId());
        if (exist == null) return Result.fail("用户不存在");
        // 防止管理员自我降权/禁用导致系统失去管理员
        if (exist.getUsername().equals(operator) && ("admin".equals(exist.getRole()))) {
            if (user.getRole() != null && !"admin".equals(user.getRole())) {
                return Result.fail("不能修改自己的管理员角色");
            }
            if (user.getStatus() != null && user.getStatus() != 1) {
                return Result.fail("不能禁用自己的账号");
            }
        }
        user.setPassword(null);
        userService.updateById(user);
        sysLogService.log(operator, "修改用户", "修改用户: " + (user.getUsername() != null ? user.getUsername() : exist.getUsername()), getIp(request));
        return Result.success("更新成功");
    }

    @RequireRole("admin")
    @DeleteMapping("/delete/{id}")
    public Result<String> delete(@PathVariable Long id, @RequestAttribute("username") String operator,
                                  HttpServletRequest request) {
        User user = userService.getById(id);
        if (user == null) return Result.fail("用户不存在");
        // 删除前检查关联的未完成单据（需求：若有则提示转移或处理）
        Long unfinishedInbound = inboundOrderMapper.selectCount(new LambdaQueryWrapper<InboundOrder>()
                .eq(InboundOrder::getApplyUser, user.getUsername())
                .eq(InboundOrder::getOrderStatus, 0));
        Long unfinishedOutbound = outboundOrderMapper.selectCount(new LambdaQueryWrapper<OutboundOrder>()
                .eq(OutboundOrder::getApplyUser, user.getUsername())
                .eq(OutboundOrder::getOrderStatus, 0));
        if (unfinishedInbound > 0 || unfinishedOutbound > 0) {
            return Result.fail("该用户名下存在" + unfinishedInbound + "张未完成入库单、" + unfinishedOutbound
                    + "张未完成出库单，请先转移或处理后再删除");
        }
        userService.removeById(id);
        sysLogService.log(operator, "删除用户", "删除用户: " + user.getUsername(), getIp(request));
        return Result.success("删除成功");
    }

    @RequireRole("admin")
    @PostMapping("/resetPwd/{id}")
    public Result<String> resetPwd(@PathVariable Long id, @RequestAttribute("username") String operator,
                                    HttpServletRequest request) {
        User user = userService.getById(id);
        if (user == null) return Result.fail("用户不存在");
        user.setPassword(passwordEncoder.encode("Abc@12345"));
        userService.updateById(user);
        sysLogService.log(operator, "重置密码", "重置用户密码: " + user.getUsername(), getIp(request));
        return Result.success("密码已重置为Abc@12345");
    }

    @GetMapping("/dingtalk/auth-url")
    public Result<Map<String, String>> dingtalkAuthUrl(@RequestParam String redirectUri) {
        String url = dingTalkLoginService.buildAuthUrl(redirectUri);
        Map<String, String> data = new HashMap<>();
        data.put("authUrl", url);
        return Result.success(data);
    }

    @PostMapping("/dingtalk/login")
    public Result<Map<String, Object>> dingtalkLogin(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String code = body.get("code");
        String state = body.get("state");
        try {
            User user = dingTalkLoginService.loginByCode(code, state);
            String token = JwtUtil.generate(user.getId(), user.getUsername(), user.getRole());
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("username", user.getUsername());
            data.put("realName", user.getRealName());
            data.put("role", user.getRole());
            loginLogService.record(user.getUsername(), getIp(request), getDevice(request), 1);
            sysLogService.log(user.getUsername(), "钉钉登录", "钉钉扫码登录", getIp(request));
            return Result.success(data);
        } catch (Exception e) {
            loginLogService.record("dingtalk", getIp(request), getDevice(request), 0);
            return Result.fail("钉钉登录失败: " + e.getMessage());
        }
    }

    private String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isEmpty()) ip = request.getRemoteAddr();
        return ip;
    }

    private String getDevice(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }
}
