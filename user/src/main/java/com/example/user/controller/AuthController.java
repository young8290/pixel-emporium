package com.example.user.controller;

import com.example.common.exception.CustomForbiddenException;
import com.example.common.exception.CustomUnauthorizedException;
import com.example.common.exception.CustomValidationException;
import com.example.common.response.R;
import com.example.common.security.JwtClaims;
import com.example.common.security.JwtUtil;
import com.example.user.config.CustomRsaComponent;
import com.example.user.entity.User;
import com.example.user.request.LoginRequest;
import com.example.user.request.RegisterRequest;
import com.example.user.response.LoginResponse;
import com.example.user.service.AuthenticationService;
import com.example.user.service.UserService;
import com.example.user.service.EmailVerificationService;
import com.example.user.response.EmailCodeResponse;
import com.example.user.request.EmailCodeRequest;
import com.example.user.util.ClientIpResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

@RestController
@RequestMapping("auth")
@Tag(name = "认证接口", description = "AuthController")
public class AuthController {
    @Autowired
    private UserService userService;

    @Autowired
    AuthenticationService authenticationService;

    @Autowired
    CustomRsaComponent customRsaComponent;

    @Autowired EmailVerificationService emailVerificationService;
    @Autowired ClientIpResolver clientIpResolver;

    @PostMapping("email-code")
    @Operation(summary = "发送注册邮箱验证码")
    public R<EmailCodeResponse> sendEmailCode(@RequestBody @Valid EmailCodeRequest request, HttpServletRequest servletRequest) {
        return new R<>(200, "若邮箱可用于注册，验证码已发送", emailVerificationService.sendRegistrationCode(request.getEmail(), clientIpResolver.resolve(servletRequest)));
    }

    @PostMapping("login")
    @Operation(summary = "用户登录")
    public R<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        return doLogin(request);
    }

    @PostMapping("register")
    @Operation(summary = "用户注册")
    public R<Boolean> register(@RequestBody @Valid RegisterRequest request) {
        String rawPassword = decryptPassword(request.getPassword());
        return R.ok(authenticationService.register(
                request.getEmail(), request.getEmailCode(), rawPassword, request.getName(), request.getMobile()));
    }

    @PostMapping("logout")
    @Operation(summary = "用户注销")
    public R<Boolean> logout(HttpServletRequest request) {
        authenticationService.logout(extractBearerToken(request));
        return R.ok(true);
    }

    @PostMapping("refresh")
    @Operation(summary = "刷新登录令牌")
    public R<LoginResponse> refresh(HttpServletRequest request) {
        String refreshedToken = authenticationService.refreshToken(extractBearerToken(request));
        Object emailPayload;
        try {
            emailPayload = JwtUtil.getClaim(refreshedToken, JwtClaims.EMAIL);
        } catch (RuntimeException e) {
            throw new CustomUnauthorizedException("刷新令牌无效");
        }
        if (emailPayload == null || String.valueOf(emailPayload).isBlank()) {
            throw new CustomUnauthorizedException("刷新令牌无效");
        }
        return R.ok(LoginResponse.builder()
                .email(String.valueOf(emailPayload))
                .token(refreshedToken)
                .build());
    }

    @PostMapping("admin/login")
    @Operation(summary = "管理端登录")
    public R<LoginResponse> adminLogin(@RequestBody @Valid LoginRequest request) {
        User user = authenticateUser(request);

        // 管理端登录需要校验管理员角色（由于登录接口不需要传递 token，无法使用 @CustomRequireRole 注解的方式校验）
        List<String> roles = userService.getRoleCodesByUserId(user.getId());
        if (roles == null || (!roles.contains("SUPER_ADMIN") && !roles.contains("ADMIN"))) {
            throw new CustomForbiddenException("权限不足，您不具备访问后台管理系统的权限");
        }

        String token = authenticationService.generateToken(user);
        LoginResponse response = LoginResponse.builder()
                .email(request.getEmail())
                .token(token)
                .build();
        return R.ok(response);
    }

    private R<LoginResponse> doLogin(LoginRequest request) {
        User user = authenticateUser(request);
        String token = authenticationService.generateToken(user);
        LoginResponse response = LoginResponse.builder()
                .email(request.getEmail())
                .token(token)
                .build();
        return R.ok(response);
    }

    private User authenticateUser(LoginRequest request) {
        String rawPassword = decryptPassword(request.getPassword());
        return authenticationService.authenticate(request.getEmail(), rawPassword);
    }

    private String decryptPassword(String encryptedPassword) {
        try {
            return customRsaComponent.decrypt(encryptedPassword);
        } catch (RuntimeException e) {
            throw new CustomValidationException("密码密文无效");
        }
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new CustomUnauthorizedException("认证不通过，用户校验失败");
        }
        String token = header.substring("Bearer ".length());
        if (token.isBlank() || token.chars().anyMatch(Character::isWhitespace)) {
            throw new CustomUnauthorizedException("认证不通过，用户校验失败");
        }
        return token;
    }
}
