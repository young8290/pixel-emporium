package com.example.user.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import cn.hutool.core.convert.Convert;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.common.exception.CustomConflictException;
import com.example.common.exception.CustomUnauthorizedException;
import com.example.common.exception.CustomValidationException;
import com.example.common.security.JwtClaims;
import com.example.common.security.JwtUtil;
import com.example.user.entity.User;
import com.example.user.mapper.UserMapper;
import com.example.user.security.PasswordPolicy;
import com.example.user.service.AuthenticationService;
import com.example.user.service.LoginAttemptService;
import com.example.user.service.TokenBlacklistService;
import com.example.user.service.UserService;
import com.example.user.service.EmailVerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {
    @Autowired
    UserService userService;

    @Autowired
    UserMapper userMapper;

    @Autowired
    TokenBlacklistService tokenBlacklistService;

    @Autowired
    LoginAttemptService loginAttemptService;

    @Autowired(required = false)
    EmailVerificationService emailVerificationService;

    @Autowired
    PasswordPolicy passwordPolicy;

    @Value("${auth.jwt.secret-key}")
    private String secretKey;

    @Value("${auth.jwt.expires-in}")
    private Long expiresIn;

    @Override
    public User authenticate(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        loginAttemptService.assertAllowed(normalizedEmail);

        QueryWrapper<User> qw = new QueryWrapper<User>().eq("email", normalizedEmail);
        User user = userService.getOne(qw);

        if (user == null || password == null || user.getPassword() == null
                || !BCrypt.checkpw(password, user.getPassword())) {
            loginAttemptService.onFailure(normalizedEmail);
            throw new CustomUnauthorizedException("邮箱或密码错误");
        }

        loginAttemptService.onSuccess(normalizedEmail);
        if (Boolean.FALSE.equals(user.getEnabled())) {
            throw new CustomUnauthorizedException("账号已停用");
        }
        return user;
    }

    @Override
    @Transactional
    public boolean register(String email, String emailCode, String rawPassword, String name, String mobile) {
        passwordPolicy.validate(rawPassword);
        if (emailVerificationService == null) {
            throw new IllegalStateException("邮箱验证码服务不可用");
        }
        String normalizedEmail = emailVerificationService.normalizeEmail(email);
        emailVerificationService.assertValid(normalizedEmail, emailCode);
        return doRegister(normalizedEmail, rawPassword, name, mobile, normalizedEmail);
    }

    private boolean doRegister(String email, String rawPassword, String name, String mobile, String verifiedEmail) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedName = normalizeText(name);
        String normalizedMobile = normalizeText(mobile);
        if (normalizedEmail.isBlank() || rawPassword == null || rawPassword.isBlank() || normalizedName.isBlank()) {
            throw new CustomValidationException("注册信息不完整");
        }

        if (exists("email", normalizedEmail)) {
            throw new CustomConflictException("邮箱已被注册");
        }
        if (exists("name", normalizedName)) {
            throw new CustomConflictException("用户名已存在");
        }
        if (!normalizedMobile.isBlank() && exists("mobile", normalizedMobile)) {
            throw new CustomConflictException("手机号已被使用");
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPassword(BCrypt.hashpw(rawPassword));
        user.setName(normalizedName);
        user.setMobile(normalizedMobile.isBlank() ? null : normalizedMobile);
        user.setEnabled(true);
        try {
            if (!userService.save(user)) {
                throw new IllegalStateException("注册用户保存失败");
            }
            int roleRows = userMapper.insertDefaultUserRole(user.getId(), IdWorker.getId());
            if (roleRows != 1) {
                throw new IllegalStateException("默认角色分配失败");
            }
            if (verifiedEmail != null && TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void afterCommit() { emailVerificationService.consume(verifiedEmail); }
                });
            }
            return true;
        } catch (DataIntegrityViolationException e) {
            throw new CustomConflictException("注册信息已存在");
        }
    }

    @Override
    public void logout(String token) {
        tokenBlacklistService.blacklist(token);
    }

    @Override
    public String refreshToken(String token) {
        if (!JwtUtil.validate(token, secretKey) || !hasSessionClaims(token)
                || tokenBlacklistService.isBlacklisted(token)) {
            throw new CustomUnauthorizedException("认证不通过，用户校验失败");
        }
        Long userId = Convert.toLong(JwtUtil.getClaim(token, JwtClaims.ID));
        if (userId == null) {
            throw new CustomUnauthorizedException("认证不通过，用户校验失败");
        }
        User user = userService.getById(userId);
        if (user == null) {
            throw new CustomUnauthorizedException("认证不通过，用户校验失败");
        }
        if (Boolean.FALSE.equals(user.getEnabled())) {
            throw new CustomUnauthorizedException("账号已停用");
        }
        Long tokenVersion = Convert.toLong(JwtUtil.getClaim(token, JwtClaims.TOKEN_VERSION), null);
        if (!normalizedTokenVersion(user).equals(tokenVersion)) {
            throw new CustomUnauthorizedException("认证不通过，用户校验失败");
        }
        String jti = JwtUtil.getJti(token);
        if (jti != null && !jti.isBlank()
                && !tokenBlacklistService.blacklistIfNotBlacklisted(token)) {
            throw new CustomUnauthorizedException("认证不通过，用户校验失败");
        }
        return generateToken(user);
    }

    @Override
    public String generateToken(User user) {
        if (user == null) {
            throw new CustomUnauthorizedException("认证不通过，用户校验失败");
        }
        if (Boolean.FALSE.equals(user.getEnabled())) {
            throw new CustomUnauthorizedException("账号已停用");
        }
        Date now = new Date();
        Date exp = new Date(now.getTime() + expiresIn);

        List<String> roles = userService.getRoleCodesByUserId(user.getId());
        List<String> permissions = userService.getPermissionCodesByUserId(user.getId());

        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaims.ID, user.getId());
        claims.put(JwtClaims.EMAIL, user.getEmail());
        claims.put(JwtClaims.JTI, UUID.randomUUID().toString());
        long proposedTokenVersion = normalizedTokenVersion(user);
        long storedTokenVersion = tokenBlacklistService.storeTokenVersion(
                user.getId(), proposedTokenVersion);
        if (storedTokenVersion != proposedTokenVersion) {
            throw new CustomUnauthorizedException("认证不通过，用户校验失败");
        }
        claims.put(JwtClaims.TOKEN_VERSION, proposedTokenVersion);
        claims.put(JwtClaims.ROLES, roles);
        claims.put(JwtClaims.PERMISSIONS, permissions);
        return JwtUtil.sign(claims, secretKey, now, exp);
    }

    @Override
    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        return JwtUtil.validate(token, secretKey) && hasSessionClaims(token)
                && !tokenBlacklistService.isBlacklisted(token);
    }

    private boolean hasSessionClaims(String token) {
        Long userId = Convert.toLong(JwtUtil.getClaim(token, JwtClaims.ID), null);
        Long tokenVersion = Convert.toLong(JwtUtil.getClaim(token, JwtClaims.TOKEN_VERSION), null);
        String jti = JwtUtil.getJti(token);
        return userId != null && tokenVersion != null && jti != null && !jti.isBlank();
    }

    private Long normalizedTokenVersion(User user) {
        return user.getTokenVersion() == null ? 0L : user.getTokenVersion();
    }

    private boolean exists(String field, String value) {
        return userService.getOne(new QueryWrapper<User>().eq(field, value)) != null;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }
}
