package com.example.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.core.UserContext;
import com.example.common.exception.CustomBusinessException;
import com.example.common.exception.CustomConflictException;
import com.example.common.exception.CustomForbiddenException;
import com.example.common.exception.CustomValidationException;
import com.example.common.response.PageResponse;
import com.example.user.entity.User;
import com.example.user.mapper.UserMapper;
import com.example.user.model.UserRoleCodeRow;
import com.example.user.request.UserListRequest;
import com.example.user.response.UserAdminResponse;
import com.example.user.service.UserService;
import com.example.user.service.TokenBlacklistService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    private final UserMapper userMapper;
    private final TokenBlacklistService tokenBlacklistService;

    public UserServiceImpl(UserMapper userMapper, TokenBlacklistService tokenBlacklistService) {
        this.userMapper = userMapper;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    public List<String> getRoleCodesByUserId(Long userId) {
        return userMapper.selectRoleCodesByUserId(userId);
    }

    @Override
    public List<String> getPermissionCodesByUserId(Long userId) {
        return userMapper.selectPermissionCodesByUserId(userId);
    }

    @Override
    public PageResponse<UserAdminResponse> adminList(UserListRequest request) {
        UserListRequest query = request == null ? new UserListRequest() : request;
        if (query.getPage() == null || query.getSize() == null) {
            throw new CustomValidationException("分页参数不能为空");
        }
        String email = StringUtils.hasText(query.getEmail()) ? query.getEmail().trim() : null;
        String name = StringUtils.hasText(query.getName()) ? query.getName().trim() : null;
        Page<User> page = userMapper.selectPage(new Page<>(query.getPage(), query.getSize()),
                new LambdaQueryWrapper<User>()
                        .like(StringUtils.hasText(email), User::getEmail, email)
                        .like(StringUtils.hasText(name), User::getName, name)
                        .eq(query.getEnabled() != null, User::getEnabled, query.getEnabled())
                        .orderByDesc(User::getCreatedAt)
                        .orderByDesc(User::getId));
        List<User> users = page.getRecords() == null ? List.of() : page.getRecords();
        Map<Long, List<String>> roleCodes = loadRoleCodes(users.stream().map(User::getId).toList());
        PageResponse<UserAdminResponse> response = new PageResponse<>();
        response.setTotal(page.getTotal());
        response.setList(users.stream()
                .map(user -> toAdminResponse(user, roleCodes.getOrDefault(user.getId(), List.of())))
                .toList());
        return response;
    }

    @Override
    public UserAdminResponse adminDetail(Long id) {
        if (id == null) {
            throw new CustomValidationException("用户ID不能为空");
        }
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new NoSuchElementException("用户不存在");
        }
        List<String> roleCodes = loadRoleCodes(List.of(id)).getOrDefault(id, List.of());
        return toAdminResponse(user, roleCodes);
    }

    @Override
    @Transactional
    public boolean updateEnabled(Long id, Boolean enabled) {
        if (id == null || enabled == null) {
            throw new CustomValidationException("用户ID和启用状态不能为空");
        }
        requireSuperAdminRoleLock();
        User user = userMapper.selectByIdForUpdate(id);
        if (user == null) {
            throw new NoSuchElementException("用户不存在");
        }
        if (Objects.equals(user.getEnabled(), enabled)) {
            return true;
        }
        List<Long> roleIds = userMapper.selectRoleIdsByUserId(id);
        if (!enabled && roleIds.contains(1L)) {
            requireCurrentSuperAdmin();
        }
        if (!enabled && roleIds.contains(1L) && userMapper.countEnabledSuperAdmins() <= 1) {
            throw new CustomConflictException("不能停用最后一个可登录的超级管理员");
        }
        if (userMapper.updateEnabledById(id, enabled) != 1) {
            throw new CustomBusinessException("用户启用状态更新失败");
        }
        user.setEnabled(enabled);
        rotateTokenVersion(id, user);
        return true;
    }

    @Override
    @Transactional
    public boolean replaceRoles(Long id, List<Long> roleIds) {
        if (id == null || roleIds == null || roleIds.isEmpty()
                || roleIds.stream().anyMatch(Objects::isNull)) {
            throw new CustomValidationException("角色ID不能为空");
        }
        List<Long> normalized = roleIds.stream().distinct().sorted().toList();
        requireCurrentSuperAdmin();
        requireSuperAdminRoleLock();
        User target = userMapper.selectByIdForUpdate(id);
        if (target == null) {
            throw new NoSuchElementException("用户不存在");
        }
        List<Long> existing = userMapper.selectExistingRoleIds(normalized);
        if (existing.size() != normalized.size() || !existing.equals(normalized)) {
            throw new CustomValidationException("包含不存在的角色");
        }
        List<Long> current = userMapper.selectRoleIdsByUserId(id);
        if (current == null) {
            current = List.of();
        }
        boolean removingSuperAdmin = current.contains(1L) && !normalized.contains(1L);
        if (removingSuperAdmin && userMapper.countSuperAdmins() <= 1) {
            throw new CustomConflictException("不能移除最后一个超级管理员");
        }
        if (removingSuperAdmin && Boolean.TRUE.equals(target.getEnabled())
                && userMapper.countEnabledSuperAdmins() <= 1) {
            throw new CustomConflictException("不能让系统失去可登录的超级管理员");
        }
        if (current.equals(normalized)) {
            return true;
        }
        if (userMapper.deleteRolesByUserId(id) != current.size()) {
            throw new CustomBusinessException("旧角色清理失败");
        }
        for (Long roleId : normalized) {
            if (userMapper.insertUserRole(id, IdWorker.getId(), roleId) != 1) {
                throw new CustomBusinessException("新角色写入失败");
            }
        }
        rotateTokenVersion(id, target);
        return true;
    }

    private void rotateTokenVersion(Long userId, User user) {
        if (userMapper.incrementTokenVersionById(userId) != 1) {
            throw new CustomBusinessException("用户会话版本更新失败");
        }
        Long tokenVersion = userMapper.selectTokenVersionById(userId);
        if (tokenVersion == null) {
            throw new CustomBusinessException("用户会话版本读取失败");
        }
        long authoritativeVersion = tokenBlacklistService.storeTokenVersion(userId, tokenVersion);
        if (authoritativeVersion != tokenVersion) {
            throw new CustomBusinessException("用户会话版本同步冲突");
        }
        user.setTokenVersion(tokenVersion);
    }

    private void requireSuperAdminRoleLock() {
        if (userMapper.lockRoleById(1L) == null) {
            throw new CustomBusinessException("SUPER_ADMIN 角色不存在");
        }
    }

    private void requireCurrentSuperAdmin() {
        Long currentUserId = UserContext.get();
        List<String> currentRoles = currentUserId == null
                ? List.of()
                : userMapper.selectRoleCodesByUserId(currentUserId);
        if (currentRoles == null || !currentRoles.contains("SUPER_ADMIN")) {
            throw new CustomForbiddenException("仅超级管理员可执行此操作");
        }
    }

    private Map<Long, List<String>> loadRoleCodes(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectRoleCodeRowsByUserIds(userIds).stream()
                .collect(Collectors.groupingBy(UserRoleCodeRow::getUserId,
                        LinkedHashMap::new,
                        Collectors.mapping(UserRoleCodeRow::getRoleCode, Collectors.toList())));
    }

    private UserAdminResponse toAdminResponse(User user, List<String> roleCodes) {
        UserAdminResponse response = new UserAdminResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setName(user.getName());
        response.setMobile(user.getMobile());
        response.setAvatar(user.getAvatar());
        response.setEnabled(user.getEnabled());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        response.setRoleCodes(List.copyOf(roleCodes));
        return response;
    }
}
