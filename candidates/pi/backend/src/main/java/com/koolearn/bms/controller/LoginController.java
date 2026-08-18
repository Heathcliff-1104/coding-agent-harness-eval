package com.koolearn.bms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.koolearn.bms.annotation.RequireRole;
import com.koolearn.bms.config.LoginRateLimiter;
import com.koolearn.bms.config.RoleInterceptor;
import com.koolearn.bms.dto.RegisterDTO;
import com.koolearn.bms.entity.InboundOrder;
import com.koolearn.bms.entity.OutboundOrder;
import com.koolearn.bms.entity.User;
import com.koolearn.bms.mapper.InboundOrderMapper;
import com.koolearn.bms.mapper.OutboundOrderMapper;
import com.koolearn.bms.service.DingTalkLoginService;
import com.koolearn.bms.service.LoginLogService;
import com.koolearn.bms.service.RoleService;
import com.koolearn.bms.service.SysOperationLogService;
import com.koolearn.bms.service.UserService;
import com.koolearn.bms.util.CaptchaUtil;
import com.koolearn.bms.util.JwtUtil;
import com.koolearn.bms.util.Result;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.validation.Valid;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.time.LocalDate;
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
    private final RoleService roleService;
    private final InboundOrderMapper inboundOrderMapper;
    private final OutboundOrderMapper outboundOrderMapper;
    private final RoleInterceptor roleInterceptor;

    @Value("${sys.default.password}")
    private String defaultPassword;

    public LoginController(UserService userService, LoginLogService loginLogService,
                           SysOperationLogService sysLogService, PasswordEncoder passwordEncoder,
                           LoginRateLimiter rateLimiter, DingTalkLoginService dingTalkLoginService,
                           RoleService roleService, InboundOrderMapper inboundOrderMapper,
                           OutboundOrderMapper outboundOrderMapper, RoleInterceptor roleInterceptor) {
        this.userService = userService;
        this.loginLogService = loginLogService;
        this.sysLogService = sysLogService;
        this.passwordEncoder = passwordEncoder;
        this.rateLimiter = rateLimiter;
        this.dingTalkLoginService = dingTalkLoginService;
        this.roleService = roleService;
        this.inboundOrderMapper = inboundOrderMapper;
        this.outboundOrderMapper = outboundOrderMapper;
        this.roleInterceptor = roleInterceptor;
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
        if (rateLimiter.isBlocked(username)) {
            return Result.fail(429, "登录过于频繁，请5分钟后再试");
        }
        if (!CaptchaUtil.verify(captchaKey, captchaCode)) {
            rateLimiter.recordAttempt(username);
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
            return Result.success(data);
        } catch (RuntimeException e) {
            rateLimiter.recordAttempt(username);
            loginLogService.record(username, getIp(request), getDevice(request), 0);
            throw e;
        }
    }

    /**
     * 注册：使用 RegisterDTO 只接收注册所需字段，
     * id/status/role/dingtalkUnionId 等字段无法从请求体批量赋值（防提权/防伪造）。
     */
    @PostMapping("/register")
    public Result<String> register(@Valid @RequestBody RegisterDTO dto, HttpServletRequest request) {
        String captchaKey = dto.getCaptchaKey();
        String captchaCode = dto.getCaptchaCode();
        if (!CaptchaUtil.verify(captchaKey, captchaCode)) {
            return Result.fail("验证码错误");
        }
        if (dto.getConfirmPassword() == null || !dto.getConfirmPassword().equals(dto.getPassword())) {
            return Result.fail("两次输入的密码不一致");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(dto.getPassword());
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setDept(dto.getDept());
        userService.register(user);
        sysLogService.log(user.getUsername(), "用户注册", "新用户注册", getIp(request));
        return Result.success("注册成功");
    }

    @GetMapping("/checkUsername")
    public Result<Map<String, Boolean>> checkUsername(@RequestParam String username) {
        Map<String, Boolean> data = new HashMap<>();
        data.put("exists", userService.count(new LambdaQueryWrapper<User>().eq(User::getUsername, username)) > 0);
        return Result.success(data);
    }

    @GetMapping("/checkPhone")
    public Result<Map<String, Boolean>> checkPhone(@RequestParam String phone) {
        Map<String, Boolean> data = new HashMap<>();
        data.put("exists", userService.count(new LambdaQueryWrapper<User>().eq(User::getPhone, phone)) > 0);
        return Result.success(data);
    }

    @RequireRole("admin")
    @PostMapping("/add")
    public Result<String> addUser(@RequestBody User user,
                                  @RequestAttribute("username") String operator,
                                  HttpServletRequest request) {
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) return Result.fail("用户名不能为空");
        if (user.getRealName() == null || user.getRealName().trim().isEmpty()) return Result.fail("真实姓名不能为空");
        if (user.getPhone() != null && !user.getPhone().isEmpty()
                && !user.getPhone().matches("^1[3-9]\\d{9}$")) return Result.fail("手机号格式不正确");
        if (userService.count(new LambdaQueryWrapper<User>().eq(User::getUsername, user.getUsername())) > 0) {
            return Result.fail("用户名已存在");
        }
        if (user.getPhone() != null && !user.getPhone().isEmpty()
                && userService.count(new LambdaQueryWrapper<User>().eq(User::getPhone, user.getPhone())) > 0) {
            return Result.fail("手机号已注册");
        }
        String pwd = (user.getPassword() == null || user.getPassword().isEmpty()) ? defaultPassword : user.getPassword();
        user.setPassword(passwordEncoder.encode(pwd));
        user.setRole(user.getRole() != null && !user.getRole().isEmpty() ? user.getRole() : "engineer");
        user.setStatus(user.getStatus() != null ? user.getStatus() : 1);
        userService.save(user);
        sysLogService.log(operator, "新增用户", "新增用户: " + user.getUsername(), getIp(request));
        return Result.success("新增成功");
    }

    @GetMapping("/info")
    public Result<Map<String, Object>> info(@RequestAttribute(value = "userId", required = false) Long userId) {
        if (userId == null) return Result.fail("未登录");
        User user = userService.getById(userId);
        if (user == null) return Result.fail("用户不存在");
        user.setPassword(null);
        Map<String, Object> data = new HashMap<>();
        data.put("user", user);
        data.put("permissions", roleService.getPermissionsByUserId(userId));
        data.put("dataScope", roleService.getDataScopeByUserId(userId));
        return Result.success(data);
    }

    @PutMapping("/changePwd")
    public Result<String> changePwd(@RequestAttribute("userId") Long userId,
                                     @RequestBody Map<String, String> body,
                                     HttpServletRequest request) {
        User user = userService.getById(userId);
        if (!passwordEncoder.matches(body.get("oldPassword"), user.getPassword())) {
            return Result.fail("原密码错误");
        }
        String newPwd = body.get("newPassword");
        String policyMsg = com.koolearn.bms.util.PasswordPolicyUtil.validate(newPwd);
        if (policyMsg != null) {
            return Result.fail(policyMsg);
        }
        user.setPassword(passwordEncoder.encode(newPwd));
        userService.updateById(user);
        sysLogService.log(user.getUsername(), "修改密码", "用户修改密码", getIp(request));
        return Result.success("修改成功");
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

    @RequireRole("admin")
    @PutMapping("/update")
    public Result<String> update(@RequestBody User user, @RequestAttribute("username") String operator,
                                  HttpServletRequest request) {
        user.setPassword(null);
        userService.updateById(user);
        // 角色/状态变更立即生效：清除该用户的角色缓存
        if (user.getId() != null) {
            roleInterceptor.evict(user.getId());
        }
        sysLogService.log(operator, "修改用户", "修改用户: " + user.getUsername(), getIp(request));
        return Result.success("更新成功");
    }

    @RequireRole("admin")
    @DeleteMapping("/delete/{id}")
    public Result<String> delete(@PathVariable Long id, @RequestAttribute("username") String operator,
                                  HttpServletRequest request) {
        User user = userService.getById(id);
        if (user == null) return Result.fail("用户不存在");
        // 存在未完成单据时禁止删除，需先转移或处理
        Long openInbound = inboundOrderMapper.selectCount(new LambdaQueryWrapper<InboundOrder>()
                .eq(InboundOrder::getApplyUser, user.getUsername())
                .eq(InboundOrder::getOrderStatus, 0));
        Long openOutbound = outboundOrderMapper.selectCount(new LambdaQueryWrapper<OutboundOrder>()
                .eq(OutboundOrder::getApplyUser, user.getUsername())
                .eq(OutboundOrder::getOrderStatus, 0));
        if ((openInbound != null && openInbound > 0) || (openOutbound != null && openOutbound > 0)) {
            return Result.fail("该用户存在未完成单据，请先转移或处理");
        }
        userService.removeById(id);
        // 删除用户后立即清除其角色缓存
        roleInterceptor.evict(id);
        sysLogService.log(operator, "删除用户", "删除用户: " + user.getUsername(), getIp(request));
        return Result.success("删除成功");
    }

    @RequireRole("admin")
    @PostMapping("/import")
    public Result<String> importUsers(@RequestParam("file") MultipartFile file,
                                      @RequestAttribute("username") String operator,
                                      HttpServletRequest request) {
        if (file == null || file.isEmpty()) return Result.fail("请上传用户Excel文件");
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            int success = 0, fail = 0;
            StringBuilder errors = new StringBuilder();
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                String username = cellStr(row, 0);
                String realName = cellStr(row, 1);
                String phone = cellStr(row, 2);
                String dept = cellStr(row, 3);
                String pwd = cellStr(row, 4);
                String role = row.getLastCellNum() > 5 ? cellStr(row, 5) : "";
                if (username.isEmpty()) continue;
                try {
                    if (userService.count(new LambdaQueryWrapper<User>().eq(User::getUsername, username)) > 0) {
                        fail++; errors.append(username).append(":用户名已存在; "); continue;
                    }
                    User u = new User();
                    u.setUsername(username);
                    u.setRealName(realName.isEmpty() ? username : realName);
                    u.setPhone(phone.isEmpty() ? null : phone);
                    u.setDept(dept.isEmpty() ? null : dept);
                    u.setPassword(passwordEncoder.encode(pwd.isEmpty() ? defaultPassword : pwd));
                    u.setRole(role.isEmpty() ? "engineer" : role);
                    u.setStatus(1);
                    userService.save(u);
                    success++;
                } catch (Exception e) {
                    fail++;
                    errors.append(username).append(":").append(e.getMessage()).append("; ");
                }
            }
            sysLogService.log(operator, "批量导入用户", "导入用户成功" + success + "失败" + fail, getIp(request));
            if (fail > 0) return Result.fail("导入完成：成功" + success + "，失败" + fail + "（" + errors + "）");
            return Result.success("导入成功 " + success + " 条");
        } catch (Exception e) {
            return Result.fail("导入失败: " + e.getMessage());
        }
    }

    @RequireRole("admin")
    @GetMapping("/export")
    public void exportUsers(HttpServletResponse response) throws Exception {
        List<User> users = userService.list(new LambdaQueryWrapper<User>().orderByDesc(User::getCreateTime));
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode("用户列表_" + LocalDate.now() + ".xlsx", "UTF-8"));
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("用户");
            String[] heads = {"用户名", "真实姓名", "手机号", "部门", "角色", "状态", "创建时间"};
            Row hr = sheet.createRow(0);
            for (int i = 0; i < heads.length; i++) hr.createCell(i).setCellValue(heads[i]);
            int n = 1;
            for (User u : users) {
                Row row = sheet.createRow(n++);
                row.createCell(0).setCellValue(u.getUsername());
                row.createCell(1).setCellValue(u.getRealName() == null ? "" : u.getRealName());
                row.createCell(2).setCellValue(u.getPhone() == null ? "" : u.getPhone());
                row.createCell(3).setCellValue(u.getDept() == null ? "" : u.getDept());
                row.createCell(4).setCellValue(u.getRole() == null ? "" : u.getRole());
                row.createCell(5).setCellValue(u.getStatus() == 1 ? "启用" : "禁用");
                row.createCell(6).setCellValue(u.getCreateTime() == null ? "" : u.getCreateTime().toString());
            }
            wb.write(response.getOutputStream());
        }
    }

    private String cellStr(Row row, int idx) {
        if (row.getCell(idx) == null) return "";
        try { return row.getCell(idx).getStringCellValue().trim(); }
        catch (Exception e) {
            try { return String.valueOf(row.getCell(idx).getNumericCellValue()).trim(); }
            catch (Exception e2) { return ""; }
        }
    }

    @RequireRole("admin")
    @PostMapping("/resetPwd/{id}")
    public Result<String> resetPwd(@PathVariable Long id, @RequestAttribute("username") String operator,
                                    HttpServletRequest request) {
        User user = userService.getById(id);
        if (user == null) return Result.fail("用户不存在");
        user.setPassword(passwordEncoder.encode(defaultPassword));
        userService.updateById(user);
        sysLogService.log(operator, "重置密码", "重置用户密码: " + user.getUsername(), getIp(request));
        return Result.success("密码已重置为" + defaultPassword);
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
