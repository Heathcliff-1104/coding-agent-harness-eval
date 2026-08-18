package com.koolearn.bms.config;

import com.koolearn.bms.annotation.RequireRole;
import com.koolearn.bms.entity.User;
import com.koolearn.bms.mapper.UserMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实时角色校验拦截器：
 * - 每个请求根据 userId 从数据库读取最新角色（忽略 JWT 中的角色声明，防止权限变更后旧 token 越权）
 * - 无论接口是否标注 @RequireRole，都把 DB 中的最新角色写入 request attribute "role"，
 *   供控制器做数据范围过滤（工程师只能看自己的单据）
 * - 使用 30 秒 TTL 的带大小上限缓存，避免每次请求都查库；管理员侧修改角色/权限时主动 evict 立即生效
 * - 用户不存在或已禁用返回 403
 */
@Component
public class RoleInterceptor implements HandlerInterceptor {

    private static final int CACHE_MAX_SIZE = 10_000;

    private final UserMapper userMapper;
    private final long cacheTtlMs;

    /** userId -> (role, expireAt) */
    private final ConcurrentHashMap<Long, Object[]> roleCache = new ConcurrentHashMap<>();

    public RoleInterceptor(UserMapper userMapper,
                           @org.springframework.beans.factory.annotation.Value("${sys.role.cache.ttl.ms:30000}") long cacheTtlMs) {
        this.userMapper = userMapper;
        this.cacheTtlMs = cacheTtlMs;
    }

    /** 供测试/管理场景手动失效某用户缓存 */
    public void evict(Long userId) {
        roleCache.remove(userId);
    }

    public void evictAll() {
        roleCache.clear();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod hm = (HandlerMethod) handler;
        RequireRole classAnno = hm.getBeanType().getAnnotation(RequireRole.class);
        RequireRole methodAnno = hm.getMethodAnnotation(RequireRole.class);

        Set<String> required = new HashSet<>();
        if (classAnno != null) required.addAll(Arrays.asList(classAnno.value()));
        if (methodAnno != null) required.addAll(Arrays.asList(methodAnno.value()));

        Object userIdAttr = request.getAttribute("userId");
        if (userIdAttr == null) {
            // 未登录（LoginInterceptor 未放行前的兜底）：无 @RequireRole 的接口也不放行内部数据
            if (required.isEmpty()) {
                return true;
            }
            forbidden(response);
            return false;
        }
        Long userId = Long.valueOf(userIdAttr.toString());
        String role = resolveRole(userId);
        // 无论是否有 @RequireRole，都把 DB 最新角色写入属性，供数据范围过滤使用
        request.setAttribute("role", role);
        if (required.isEmpty()) {
            return true;
        }
        if (role == null || !required.contains(role)) {
            // 用户不存在或已禁用，或角色不满足要求
            forbidden(response);
            return false;
        }
        return true;
    }

    private String resolveRole(Long userId) {
        long now = System.currentTimeMillis();
        Object[] cached = roleCache.get(userId);
        if (cached != null && (long) cached[1] > now) {
            return (String) cached[0];
        }
        if (cached != null) {
            roleCache.remove(userId);
        }
        User user = userMapper.selectById(userId);
        String role = null;
        if (user != null && user.getStatus() != null && user.getStatus() == 1) {
            role = user.getRole();
        }
        if (role != null && roleCache.size() < CACHE_MAX_SIZE) {
            roleCache.put(userId, new Object[]{role, now + cacheTtlMs});
        }
        return role;
    }

    private void forbidden(HttpServletResponse response) throws Exception {
        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":403,\"msg\":\"权限不足\"}");
    }
}
