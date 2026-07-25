package com.example.business.aspect;

import com.example.business.service.OperationLogService;
import com.example.common.core.UserContext;
import com.example.common.response.R;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * 操作审计切面（BE-P2-01）。
 *
 * <p>拦截 {@link com.example.business.annotation.OperationLog} 注解方法：方法正常返回记
 * {@code SUCCESS}，抛异常记 {@code FAIL} 后重新抛出。审计写入是异步的
 * （{@link OperationLogService#record}），绝不影响主业务流程。
 *
 * <p>操作人取自 {@link UserContext}（LoginInterceptor 注入的登录用户 id）；
 * IP 从当前请求解析（优先 X-Forwarded-For）。targetId 通过注解上的 SpEL 基于方法入参求值。
 */
@Aspect
@Component
public class OperationLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperationLogAspect.class);

    private final OperationLogService operationLogService;

    private final ExpressionParser spelParser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public OperationLogAspect(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @Around("@annotation(com.example.business.annotation.OperationLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        com.example.business.annotation.OperationLog annotation =
                method.getAnnotation(com.example.business.annotation.OperationLog.class);

        Object result;
        String outcome = "SUCCESS";
        try {
            result = joinPoint.proceed();
            if (result instanceof R<?> response
                    && (response.getCode() == null || response.getCode() != 200)) {
                outcome = "FAIL";
            } else if (result instanceof Map<?, ?> response
                    && response.get("code") instanceof Number code
                    && code.intValue() != 0) {
                outcome = "FAIL";
            }
            return result;
        } catch (Throwable ex) {
            outcome = "FAIL";
            throw ex;
        } finally {
            try {
                record(annotation, method, joinPoint.getArgs(), outcome);
            } catch (Exception e) {
                // 审计构造失败绝不影响主流程
                log.warn("构造操作审计记录失败: action={}, err={}", annotation.action(), e.getMessage());
            }
        }
    }

    private void record(com.example.business.annotation.OperationLog annotation, Method method,
                        Object[] args, String outcome) {
        com.example.business.entity.OperationLog entity = new com.example.business.entity.OperationLog();
        entity.setUserId(UserContext.get());
        entity.setAction(annotation.action());
        entity.setTargetType(blankToNull(annotation.targetType()));
        entity.setTargetId(resolveTargetId(annotation.targetIdSpEL(), method, args));
        entity.setDetail(blankToNull(annotation.detail()));
        entity.setIp(resolveIp());
        entity.setResult(outcome);
        operationLogService.record(entity);
    }

    /**
     * 用 SpEL 基于方法入参求 targetId（如 "#id" / "#request.id"）。求值失败或表达式为空返回 null。
     */
    private String resolveTargetId(String spel, Method method, Object[] args) {
        if (spel == null || spel.isBlank()) {
            return null;
        }
        try {
            StandardEvaluationContext ctx = new StandardEvaluationContext();
            String[] names = parameterNameDiscoverer.getParameterNames(method);
            if (names != null) {
                for (int i = 0; i < names.length && i < args.length; i++) {
                    ctx.setVariable(names[i], args[i]);
                }
            }
            Expression expr = spelParser.parseExpression(spel);
            Object value = expr.getValue(ctx);
            return value == null ? null : String.valueOf(value);
        } catch (Exception e) {
            log.debug("解析 targetId SpEL 失败: {}", spel);
            return null;
        }
    }

    /**
     * 解析请求来源 IP：优先 X-Forwarded-For 首段，否则 remoteAddr。无请求上下文返回 null。
     */
    private String resolveIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return null;
            }
            HttpServletRequest request = attrs.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                int comma = forwarded.indexOf(',');
                return comma > 0 ? forwarded.substring(0, comma).trim() : forwarded.trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
