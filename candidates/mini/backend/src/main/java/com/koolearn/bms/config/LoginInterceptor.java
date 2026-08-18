package com.koolearn.bms.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.koolearn.bms.entity.User;
import com.koolearn.bms.mapper.UserMapper;
import com.koolearn.bms.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    public LoginInterceptor(JwtUtil jwtUtil, UserMapper userMapper) {
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            return reject(response, 401, "未登录");
        }
        try {
            Claims claims = jwtUtil.parse(token);
            Object userIdObj = claims.get("userId");
            if (userIdObj == null) {
                return reject(response, 401, "未登录");
            }
            Long userId = Long.valueOf(userIdObj.toString());
            // 实时校验：每次请求都从数据库读取当前用户角色与状态，确保权限变更立即生效、禁用用户立即失效
            User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getId, userId));
            if (user == null) {
                return reject(response, 401, "用户不存在");
            }
            if (user.getStatus() == null || user.getStatus() != 1) {
                return reject(response, 401, "账号已被禁用");
            }
            request.setAttribute("userId", user.getId());
            request.setAttribute("username", user.getUsername());
            request.setAttribute("role", user.getRole());
            return true;
        } catch (Exception e) {
            return reject(response, 401, "登录已过期");
        }
    }

    private boolean reject(HttpServletResponse response, int code, String msg) throws Exception {
        response.setStatus(code);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + code + ",\"msg\":\"" + msg + "\"}");
        return false;
    }
}
