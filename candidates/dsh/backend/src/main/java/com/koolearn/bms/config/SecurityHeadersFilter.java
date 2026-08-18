package com.koolearn.bms.config;

import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 基础安全响应头（安全审计 L1）：
 * X-Frame-Options / X-Content-Type-Options / Referrer-Policy 等，防止点击劫持与 MIME 嗅探。
 */
@Component
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (response instanceof HttpServletResponse) {
            HttpServletResponse resp = (HttpServletResponse) response;
            resp.setHeader("X-Frame-Options", "SAMEORIGIN");
            resp.setHeader("X-Content-Type-Options", "nosniff");
            resp.setHeader("Referrer-Policy", "same-origin");
            resp.setHeader("X-XSS-Protection", "1; mode=block");
            resp.setHeader("Cache-Control", "no-store");
        }
        chain.doFilter(request, response);
    }
}
