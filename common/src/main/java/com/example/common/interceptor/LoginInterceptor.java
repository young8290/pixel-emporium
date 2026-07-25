package com.example.common.interceptor;

import cn.hutool.core.convert.Convert;
import com.example.common.annotation.CustomRequirePermission;
import com.example.common.annotation.CustomRequireRole;
import com.example.common.core.UserContext;
import com.example.common.exception.CustomForbiddenException;
import com.example.common.exception.CustomUnauthorizedException;
import com.example.common.security.JwtClaims;
import com.example.common.security.JwtUtil;
import com.example.common.security.TokenBlacklistChecker;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;

@Component
@Lazy
public class LoginInterceptor implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(LoginInterceptor.class);

    @Value("${auth.jwt.secret-key}")
    private String secretKey;

    @Autowired
    private TokenBlacklistChecker tokenBlacklistChecker;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        UserContext.remove();
        // 从用户的请求头中获取 token
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 校验 token
        boolean b = validateToken(token);
        if (b) {
            if (tokenBlacklistChecker == null) {
                throw new IllegalStateException("令牌状态检查器未配置");
            }
            if (tokenBlacklistChecker.isBlacklisted(token)) {
                throw new CustomUnauthorizedException("认证不通过，用户校验失败");
            }

            // 从 token 中解析出之前设置的用户信息 [.setPayload("xxx", xxx)]
            Object idPayload = JwtUtil.getClaim(token, JwtClaims.ID);
            Long id = Convert.toLong(idPayload);

            // 校验角色和权限：读取注解，校验用户角色/权限
            if (handler instanceof HandlerMethod handlerMethod) {
                // 角色校验
                CustomRequireRole requireRole = getAnnotation(handlerMethod, CustomRequireRole.class);
                if (requireRole != null) {
                    List<String> userRoles = JwtUtil.getRoles(token);
                    String[] requiredRoles = requireRole.value();

                    boolean hasRole = userRoles != null && Arrays.stream(requiredRoles).anyMatch(userRoles::contains);
                    if (!hasRole) {
                        log.error("角色不足，拦截请求 {}，需要角色 {}，用户角色 {}",
                                request.getRequestURI(), List.of(requiredRoles), userRoles);
                        throw new CustomForbiddenException("权限不足，拒绝访问");
                    }
                }

                // 权限校验
                CustomRequirePermission requirePermission = getAnnotation(handlerMethod, CustomRequirePermission.class);
                if (requirePermission != null) {
                    Object permissionsPayload = JwtUtil.getClaim(token, JwtClaims.PERMISSIONS);
                    List<String> userPermissions = Convert.toList(String.class, permissionsPayload);
                    String[] requiredPermissions = requirePermission.value();

                    boolean hasPermission = userPermissions != null
                            && Arrays.stream(requiredPermissions).anyMatch(userPermissions::contains);
                    if (!hasPermission) {
                        log.error("权限不足，拦截请求 {}，需要权限 {}，用户权限 {}",
                                request.getRequestURI(), List.of(requiredPermissions), userPermissions);
                        throw new CustomForbiddenException("权限不足，拒绝访问");
                    }
                }
            }

            UserContext.set(id);
            return true;
        }

        throw new CustomUnauthorizedException("认证不通过，用户校验失败");
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求结束后必须清理 ThreadLocal，防止线程重用导致的数据污染
        UserContext.remove();
    }

    private boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        return JwtUtil.validate(token, secretKey);
    }

    /**
     * 从方法或类上获取注解，方法级优先于类级。
     */
    private <A extends Annotation> A getAnnotation(HandlerMethod handlerMethod, Class<A> annotationType) {
        A annotation = handlerMethod.getMethodAnnotation(annotationType);
        if (annotation == null) {
            annotation = handlerMethod.getBeanType().getAnnotation(annotationType);
        }
        return annotation;
    }
}
