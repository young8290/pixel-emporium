package com.example.user.aspect;

import com.example.common.core.UserContext;
import com.example.common.response.R;
import com.example.user.annotation.OperationLog;
import com.example.user.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

@Aspect
@Component
public class OperationLogAspect {
    private static final Logger log = LoggerFactory.getLogger(OperationLogAspect.class);

    private final OperationLogService operationLogService;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    public OperationLogAspect(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @Around("@annotation(com.example.user.annotation.OperationLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        OperationLog annotation = method.getAnnotation(OperationLog.class);
        String outcome = "SUCCESS";
        try {
            Object result = joinPoint.proceed();
            if (result instanceof R<?> response
                    && (response.getCode() == null || response.getCode() != 200)) {
                outcome = "FAIL";
            }
            return result;
        } catch (Throwable exception) {
            outcome = "FAIL";
            throw exception;
        } finally {
            try {
                record(annotation, method, joinPoint.getArgs(), outcome);
            } catch (Exception exception) {
                log.warn("构造用户操作审计失败: action={}, err={}",
                        annotation.action(), exception.getMessage());
            }
        }
    }

    private void record(OperationLog annotation, Method method, Object[] args, String outcome) {
        com.example.user.entity.OperationLog entity = new com.example.user.entity.OperationLog();
        entity.setUserId(UserContext.get());
        entity.setAction(annotation.action());
        entity.setTargetType(blankToNull(annotation.targetType()));
        entity.setTargetId(resolveTargetId(annotation.targetIdSpEL(), method, args));
        entity.setDetail(blankToNull(annotation.detail()));
        entity.setIp(resolveIp());
        entity.setResult(outcome);
        operationLogService.record(entity);
    }

    private String resolveTargetId(String expression, Method method, Object[] args) {
        if (expression == null || expression.isBlank()) {
            return null;
        }
        try {
            StandardEvaluationContext context = new StandardEvaluationContext();
            String[] names = nameDiscoverer.getParameterNames(method);
            if (names != null) {
                for (int i = 0; i < names.length && i < args.length; i++) {
                    context.setVariable(names[i], args[i]);
                }
            }
            Object value = parser.parseExpression(expression).getValue(context);
            return value == null ? null : String.valueOf(value);
        } catch (Exception exception) {
            log.debug("解析用户审计 targetId 失败: {}", expression);
            return null;
        }
    }

    private String resolveIp() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return null;
            }
            HttpServletRequest request = attributes.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                int comma = forwarded.indexOf(',');
                return comma > 0 ? forwarded.substring(0, comma).trim() : forwarded.trim();
            }
            return request.getRemoteAddr();
        } catch (Exception exception) {
            return null;
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
