package com.example.business.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作审计注解（BE-P2-01）。
 *
 * <p>标注在管理端关键操作方法上，由 {@code OperationLogAspect} 切面拦截，
 * 记录一条 {@code operation_log}（操作人、动作、目标、IP、结果）。
 *
 * <p>示例：
 * <pre>{@code
 * @OperationLog(action = "ORDER_SHIP", targetType = "ORDER", targetIdSpEL = "#id")
 * public Boolean ship(Long id) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {

    /** 操作动作，如 ORDER_SHIP / USER_ROLE_UPDATE / PRODUCT_DELETE。 */
    String action();

    /** 目标类型，如 ORDER / USER / PRODUCT / CATEGORY / SKU。 */
    String targetType() default "";

    /**
     * 目标 ID 的 SpEL 表达式（基于方法入参），如 {@code "#id"} 或 {@code "#request.id"}。
     * 留空则不记录 targetId。
     */
    String targetIdSpEL() default "";

    /** 操作详情描述（静态文案；敏感信息不入此字段）。 */
    String detail() default "";
}
