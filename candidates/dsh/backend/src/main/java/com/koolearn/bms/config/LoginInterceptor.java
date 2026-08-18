package com.koolearn.bms.config;

import com.koolearn.bms.entity.User;
import com.koolearn.bms.mapper.UserMapper;
import com.koolearn.bms.service.PermissionService;
import com.koolearn.bms.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Set;

/**
 * 登录拦截器：JWT 仅用于身份凭证；每次请求从数据库重新校验用户状态、角色，
 * 并加载最新权限集合，满足“后端根据用户名查询用户角色及权限列表、每次请求重新校验”的要求，
 * 权限变更实时生效（无需重新登录）。
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    public static final String ATTR_USER_ID = "userId";
    public static final String ATTR_USERNAME = "username";
    public static final String ATTR_ROLE = "role";
    public static final String ATTR_PERMISSIONS = "permissions";
    public static final String ATTR_DATA_SCOPE = "dataScope";

    private final UserMapper userMapper;
    private final PermissionService permissionService;

    public LoginInterceptor(UserMapper userMapper, PermissionService permissionService) {
        this.userMapper = userMapper;
        this.permissionService = permissionService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            writeUnauthorized(response, "未登录");
            return false;
        }
        try {
            Claims claims = JwtUtil.parse(token);
            Object userIdObj = claims.get("userId");
            if (userIdObj == null) {
                writeUnauthorized(response, "登录已过期");
                return false;
            }
            Long userId = Long.valueOf(userIdObj.toString());
            // 每次请求从数据库重新校验用户状态与权限（权限变更实时生效）
            User user = userMapper.selectById(userId);
            if (user == null || user.getStatus() == null || user.getStatus() != 1) {
                writeUnauthorized(response, "账号不存在或已被禁用");
                return false;
            }
            Set<String> permissions = permissionService.loadPermissionCodes(userId);
            String dataScope = permissionService.loadDataScope(userId);
            request.setAttribute(ATTR_USER_ID, userId);
            request.setAttribute(ATTR_USERNAME, user.getUsername());
            request.setAttribute(ATTR_ROLE, user.getRole());
            request.setAttribute(ATTR_PERMISSIONS, permissions);
            request.setAttribute(ATTR_DATA_SCOPE, dataScope);
            return true;
        } catch (Exception e) {
            writeUnauthorized(response, "登录已过期");
            return false;
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String msg) throws Exception {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"msg\":\"" + msg + "\"}");
    }
}
