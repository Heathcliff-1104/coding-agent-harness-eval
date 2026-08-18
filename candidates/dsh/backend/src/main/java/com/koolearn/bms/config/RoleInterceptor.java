package com.koolearn.bms.config;

import com.koolearn.bms.annotation.RequirePermission;
import com.koolearn.bms.annotation.RequireRole;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Component
public class RoleInterceptor implements HandlerInterceptor {

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

        if (!required.isEmpty()) {
            String role = (String) request.getAttribute(LoginInterceptor.ATTR_ROLE);
            if (role == null || !required.contains(role)) {
                response.setStatus(403);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":403,\"msg\":\"权限不足\"}");
                return false;
            }
        }

        // 权限码校验（从数据库加载的最新权限集合）
        RequirePermission classPerm = hm.getBeanType().getAnnotation(RequirePermission.class);
        RequirePermission methodPerm = hm.getMethodAnnotation(RequirePermission.class);
        String requiredPerm = null;
        if (classPerm != null) requiredPerm = classPerm.value();
        if (methodPerm != null) requiredPerm = methodPerm.value();
        if (requiredPerm != null) {
            @SuppressWarnings("unchecked")
            Set<String> permissions = (Set<String>) request.getAttribute(LoginInterceptor.ATTR_PERMISSIONS);
            if (permissions == null || !permissions.contains(requiredPerm)) {
                response.setStatus(403);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":403,\"msg\":\"权限不足\"}");
                return false;
            }
        }
        return true;
    }
}
