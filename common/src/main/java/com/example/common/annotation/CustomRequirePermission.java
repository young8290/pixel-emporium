package com.example.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明式细粒度权限校验注解。
 * 标注在 Controller 类或方法上，LoginInterceptor 会自动校验当前用户是否具备指定权限。
 * <p>
 * 与 {@link CustomRequireRole} 互补：角色是粗粒度分类，权限是细粒度操作。
 * 方法级注解优先级高于类级注解。
 * <p>
 * 示例：
 * <pre>
 * &#64;CustomRequirePermission("user:list")
 * public R&lt;Object&gt; list() { ... }
 *
 * &#64;CustomRequirePermission({"user:add", "user:update"})
 * public R&lt;Object&gt; modify() { ... }
 * </pre>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CustomRequirePermission {

    /**
     * 允许访问的权限编码列表，用户必须至少拥有其中一个权限。
     */
    String[] value();
}
