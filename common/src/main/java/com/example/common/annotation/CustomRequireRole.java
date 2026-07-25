package com.example.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明式角色权限校验注解。
 * 标注在 Controller 类或方法上，LoginInterceptor 会自动校验当前用户是否具备指定角色。
 * <p>
 * 方法级注解优先级高于类级注解。
 * <p>
 * 示例：
 * <pre>
 * &#64;CustomRequireRole({"ADMIN", "SUPER_ADMIN"})
 * public class UserAdminController { ... }
 *
 * &#64;CustomRequireRole("VIP")
 * public R&lt;Object&gt; vipOnly() { ... }
 * </pre>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CustomRequireRole {

    /**
     * 允许访问的角色列表，用户必须至少拥有其中一个角色。
     */
    String[] value();
}
