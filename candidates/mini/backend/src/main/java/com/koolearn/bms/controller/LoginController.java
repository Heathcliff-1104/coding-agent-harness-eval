package com.koolearn.bms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.koolearn.bms.annotation.RequireRole;
import com.koolearn.bms.config.LoginRateLimiter;
import com.koolearn.bms.dto.UserRegisterDTO;
import com.koolearn.bms.entity.User;
import com.koolearn.bms.service.DingTalkLoginService;
import com.koolearn.bms.service.LoginLogService;
import com.koolearn.bms.service.SysOperationLogService;
import com.koolearn.bms.service.UserService;
import com.koolearn.bms.util.CaptchaUtil;
import com.koolearn.bms.util.JwtUtil;
import com.koolearn.bms.util.Result;
import com.koolearn.bms.entity.InboundOrder;
import com.koolearn.bms.entity.OutboundOrder;
import com.koolearn.bms.service.InboundOrderService;
import com.koolearn.bms.service.OutboundOrderService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.password.PasswordEncoder;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
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
    private final JwtUtil jwtUtil;
    private final InboundOrderService inboundOrderService;
    private final OutboundOrderService outboundOrderService;

    public LoginController(UserService userService, LoginLogService loginLogService,
                           SysOperationLogService sysLogService, PasswordEncoder passwordEncoder,
                           LoginRateLimiter rateLimiter, DingTalkLoginService dingTalkLoginService,
                           JwtUtil jwtUtil, InboundOrderService inboundOrderService,
                           OutboundOrderService outboundOrderService) {
        this.userService = userService;
        this.loginLogService = loginLogService;
        this.sysLogService = sysLogService;
        this.passwordEncoder = passwordEncoder;
        this.rateLimiter = rateLimiter;
        this.dingTalkLoginService = dingTalkLoginService;
        this.jwtUtil = jwtUtil;
        this.inboundOrderService = inboundOrderService;
        this.outboundOrderService = outboundOrderService;
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
        String rateKey = username + "|" + getIp(request);
        if (rateLimiter.isBlocked(rateKey)) {
            return Result.fail(429, "登录过于频繁，请5分钟后再试");
        }
        if (!CaptchaUtil.verify(captchaKey, captchaCode)) {
            rateLimiter.recordAttempt(rateKey);
            loginLogService.record(username, getIp(request), getDevice(request), 0);
            return Result.fail("验证码错误");
        }
        try {
            User user = userService.login(username, password);
            String token = jwtUtil.generate(user.getId(), user.getUsername(), user.getRole());
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("username", user.getUsername());
            data.put("realName", user.getRealName());
            data.put("role", user.getRole());
            loginLogService.record(username, getIp(request), getDevice(request), 1);
            return Result.success(data);
        } catch (RuntimeException e) {
            rateLimiter.recordAttempt(rateKey);
            loginLogService.record(username, getIp(request), getDevice(request), 0);
            throw e;
        }
    }

    @PostMapping("/register")
    public Result<String> register(@Valid @RequestBody UserRegisterDTO dto, HttpServletRequest request) {
        userService.register(dto);
        sysLogService.log(dto.getUsername(), "用户注册", "新用户注册", getIp(request));
        return Result.success("注册成功");
    }

    @GetMapping("/info")
    public Result<User> info(@RequestAttribute(value = "userId", required = false) Long userId) {
        if (userId == null) return Result.fail("未登录");
        User user = userService.getById(userId);
        if (user != null) user.setPassword(null);
        return Result.success(user);
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
        if (newPwd == null || newPwd.length() < 8) return Result.fail("新密码长度不足8位");
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
        sysLogService.log(operator, "修改用户", "修改用户: " + user.getUsername(), getIp(request));
        return Result.success("更新成功");
    }

    @RequireRole("admin")
    @DeleteMapping("/delete/{id}")
    public Result<String> delete(@PathVariable Long id, @RequestAttribute("username") String operator,
                                  HttpServletRequest request) {
        User user = userService.getById(id);
        if (user == null) return Result.fail("用户不存在");
        // 检查关联未完成单据
        long pendingInbound = inboundOrderService.count(new LambdaQueryWrapper<InboundOrder>()
                .eq(InboundOrder::getApplyUser, user.getUsername()).eq(InboundOrder::getOrderStatus, 0));
        long pendingOutbound = outboundOrderService.count(new LambdaQueryWrapper<OutboundOrder>()
                .eq(OutboundOrder::getApplyUser, user.getUsername()).eq(OutboundOrder::getOrderStatus, 0));
        if (pendingInbound + pendingOutbound > 0) {
            return Result.fail("该用户存在未完成单据（入库" + pendingInbound + "，出库" + pendingOutbound + "），请先处理或转移");
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
        user.setPassword(passwordEncoder.encode("12345678"));
        userService.updateById(user);
        sysLogService.log(operator, "重置密码", "重置用户密码: " + user.getUsername(), getIp(request));
        return Result.success("密码已重置为12345678");
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
            String token = jwtUtil.generate(user.getId(), user.getUsername(), user.getRole());
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


    @RequireRole("admin")
    @PostMapping("/admin/create")
    public Result<String> adminCreate(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String username = body.get("username");
        String password = body.get("password");
        String realName = body.get("realName");
        String phone = body.get("phone");
        String dept = body.get("dept");
        String role = body.get("role");
        if (username == null || username.trim().isEmpty()) return Result.fail("用户名不能为空");
        if (password == null || password.length() < 8 || password.length() > 20) {
            return Result.fail("密码长度须为8~20位");
        }
        if (realName == null || realName.trim().isEmpty()) return Result.fail("真实姓名不能为空");
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) return Result.fail("手机号格式不正确");
        if (role == null || role.trim().isEmpty()) role = "engineer";

        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername(username.trim());
        dto.setPassword(password);
        dto.setRealName(realName);
        dto.setPhone(phone);
        dto.setDept(dept);
        // 注册默认工程师，随后覆盖角色
        userService.register(dto);
        User user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username.trim()));
        if (user != null) {
            user.setRole(role);
            userService.updateById(user);
        }
        sysLogService.log(body.get("operator") != null ? body.get("operator") : "admin", "新增用户", "管理员新增用户: " + username, getIp(request));
        return Result.success("新增用户成功");
    }

    @RequireRole("admin")
    @GetMapping("/export")
    public void exportUsers(@RequestParam(required = false) String keyword, HttpServletResponse response) throws Exception {
        List<User> users = userService.list(new LambdaQueryWrapper<User>()
                .like(keyword != null && !keyword.isEmpty(), User::getUsername, keyword)
                .orderByDesc(User::getCreateTime));
        users.forEach(u -> u.setPassword(null));
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode("用户列表_" + LocalDate.now() + ".xlsx", "UTF-8"));
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("用户");
        String[] headers = {"用户名", "真实姓名", "部门", "手机号", "角色", "状态", "创建时间"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) headerRow.createCell(i).setCellValue(headers[i]);
        int rowNum = 1;
        for (User u : users) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(u.getUsername() == null ? "" : u.getUsername());
            row.createCell(1).setCellValue(u.getRealName() == null ? "" : u.getRealName());
            row.createCell(2).setCellValue(u.getDept() == null ? "" : u.getDept());
            row.createCell(3).setCellValue(u.getPhone() == null ? "" : u.getPhone());
            row.createCell(4).setCellValue(u.getRole() == null ? "" : u.getRole());
            row.createCell(5).setCellValue(u.getStatus() != null && u.getStatus() == 1 ? "启用" : "禁用");
            row.createCell(6).setCellValue(u.getCreateTime() == null ? "" : u.getCreateTime().toString());
        }
        try (OutputStream os = response.getOutputStream()) {
            wb.write(os);
        }
        wb.close();
    }

    @RequireRole("admin")
    @PostMapping("/import")
    public Result<String> importUsers(@RequestParam("file") MultipartFile file, HttpServletRequest request) throws Exception {
        Workbook wb = new XSSFWorkbook(file.getInputStream());
        Sheet sheet = wb.getSheetAt(0);
        int ok = 0;
        int fail = 0;
        StringBuilder errs = new StringBuilder();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            String username = getCell(row, 0);
            String realName = getCell(row, 1);
            String dept = getCell(row, 2);
            String phone = getCell(row, 3);
            String role = getCell(row, 4);
            String password = "12345678";
            if (username.isEmpty() || realName.isEmpty()) {
                fail++;
                continue;
            }
            try {
                UserRegisterDTO dto = new UserRegisterDTO();
                dto.setUsername(username);
                dto.setPassword(password);
                dto.setRealName(realName);
                dto.setPhone(phone);
                dto.setDept(dept);
                userService.register(dto);
                User user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
                if (user != null && !role.isEmpty()) {
                    user.setRole(role);
                    userService.updateById(user);
                }
                ok++;
            } catch (Exception e) {
                fail++;
                if (errs.length() < 500) errs.append(username).append(":").append(e.getMessage()).append("; ");
            }
        }
        wb.close();
        sysLogService.log("admin", "批量导入用户", "成功" + ok + " 失败" + fail, getIp(request));
        return Result.success("导入完成，成功" + ok + "条，失败" + fail + "条" + (errs.length() > 0 ? "：" + errs : ""));
    }

    private String getCell(Row row, int idx) {
        Cell cell = row.getCell(idx);
        if (cell == null) return "";
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue() == null ? "" : cell.getStringCellValue().trim();
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
