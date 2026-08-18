package com.koolearn.bms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.koolearn.bms.annotation.RequireRole;
import com.koolearn.bms.config.LoginRateLimiter;
import com.koolearn.bms.entity.User;
import com.koolearn.bms.service.DingTalkLoginService;
import com.koolearn.bms.service.LoginLogService;
import com.koolearn.bms.service.SysOperationLogService;
import com.koolearn.bms.service.UserService;
import com.koolearn.bms.util.CaptchaUtil;
import com.koolearn.bms.util.JwtUtil;
import com.koolearn.bms.util.Result;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
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

    public LoginController(UserService userService, LoginLogService loginLogService,
                           SysOperationLogService sysLogService, PasswordEncoder passwordEncoder,
                           LoginRateLimiter rateLimiter, DingTalkLoginService dingTalkLoginService) {
        this.userService = userService;
        this.loginLogService = loginLogService;
        this.sysLogService = sysLogService;
        this.passwordEncoder = passwordEncoder;
        this.rateLimiter = rateLimiter;
        this.dingTalkLoginService = dingTalkLoginService;
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

    @PostMapping("/register")
    public Result<String> register(@RequestBody User user, HttpServletRequest request) {
        userService.register(user);
        sysLogService.log(user.getUsername(), "用户注册", "新用户注册", getIp(request));
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
        if (user != null) {
            userService.removeById(id);
            sysLogService.log(operator, "删除用户", "删除用户: " + user.getUsername(), getIp(request));
        }
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
