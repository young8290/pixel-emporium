package com.example.user.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.example.common.core.UserContext;
import com.example.common.exception.CustomBusinessException;
import com.example.common.exception.CustomConflictException;
import com.example.common.exception.CustomUnauthorizedException;
import com.example.common.exception.CustomValidationException;
import com.example.user.entity.User;
import com.example.user.mapper.UserMapper;
import com.example.user.request.ProfileUpdateRequest;
import com.example.user.response.ProfileResponse;
import com.example.user.security.PasswordPolicy;
import com.example.user.service.AccountService;
import com.example.user.service.TokenBlacklistService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.NoSuchElementException;

@Service
public class AccountServiceImpl implements AccountService {
    private final UserMapper userMapper;
    private final TokenBlacklistService tokenBlacklistService;
    private final PasswordPolicy passwordPolicy;

    public AccountServiceImpl(UserMapper userMapper,
                              TokenBlacklistService tokenBlacklistService,
                              PasswordPolicy passwordPolicy) {
        this.userMapper = userMapper;
        this.tokenBlacklistService = tokenBlacklistService;
        this.passwordPolicy = passwordPolicy;
    }

    @Override
    public ProfileResponse profile() {
        return response(currentUser());
    }

    @Override
    @Transactional
    public ProfileResponse updateProfile(ProfileUpdateRequest request) {
        Long userId = currentUserId();
        String name = request.getName().trim();
        String mobile = normalizeNullable(request.getMobile());
        String avatar = normalizeNullable(request.getAvatar());
        if (userMapper.countActiveByNameExcludingId(name, userId) > 0) {
            throw new CustomConflictException("昵称已被使用");
        }
        if (mobile != null && userMapper.countActiveByMobileExcludingId(mobile, userId) > 0) {
            throw new CustomConflictException("手机号已被使用");
        }
        try {
            if (userMapper.updateProfileById(userId, name, mobile, avatar) != 1) {
                throw new CustomBusinessException("个人资料更新失败");
            }
        } catch (DataIntegrityViolationException exception) {
            throw new CustomConflictException("昵称或手机号已被使用");
        }
        return response(currentUser());
    }

    @Override
    @Transactional
    public void changePassword(String currentPassword, String newPassword) {
        Long userId = currentUserId();
        User user = userMapper.selectByIdForUpdate(userId);
        if (user == null) {
            throw new NoSuchElementException("用户不存在");
        }
        if (currentPassword == null || user.getPassword() == null
                || !BCrypt.checkpw(currentPassword, user.getPassword())) {
            throw new CustomValidationException("当前密码不正确");
        }
        passwordPolicy.validate(newPassword);
        if (BCrypt.checkpw(newPassword, user.getPassword())) {
            throw new CustomValidationException("新密码不能与当前密码相同");
        }
        if (userMapper.updatePasswordAndIncrementTokenVersion(userId, BCrypt.hashpw(newPassword)) != 1) {
            throw new CustomBusinessException("密码更新失败");
        }
        Long tokenVersion = userMapper.selectTokenVersionById(userId);
        if (tokenVersion == null) {
            throw new CustomBusinessException("用户会话版本读取失败");
        }
        long authoritativeVersion = tokenBlacklistService.storeTokenVersion(userId, tokenVersion);
        if (authoritativeVersion != tokenVersion) {
            throw new CustomBusinessException("用户会话版本同步冲突");
        }
    }

    private User currentUser() {
        User user = userMapper.selectById(currentUserId());
        if (user == null) {
            throw new NoSuchElementException("用户不存在");
        }
        return user;
    }

    private Long currentUserId() {
        Long userId = UserContext.get();
        if (userId == null) {
            throw new CustomUnauthorizedException("认证不通过，用户校验失败");
        }
        return userId;
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private ProfileResponse response(User user) {
        ProfileResponse response = new ProfileResponse();
        response.setEmail(user.getEmail());
        response.setName(user.getName());
        response.setMobile(user.getMobile());
        response.setAvatar(user.getAvatar());
        return response;
    }
}
