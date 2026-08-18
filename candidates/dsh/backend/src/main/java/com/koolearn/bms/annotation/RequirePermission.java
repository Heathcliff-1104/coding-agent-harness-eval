package com.koolearn.bms.annotation;

import java.lang.annotation.*;

/**
 * 权限码校验注解（配合 LoginInterceptor 每次请求从数据库加载的权限集合）。
 * 例：@RequirePermission("btn:inbound:confirm")
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {
    String value();
}
