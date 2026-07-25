package com.example.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.user.entity.User;
import com.example.common.response.PageResponse;
import com.example.user.request.UserListRequest;
import com.example.user.response.UserAdminResponse;

import java.util.List;

public interface UserService extends IService<User> {
    List<String> getRoleCodesByUserId(Long userId);

    List<String> getPermissionCodesByUserId(Long userId);

    PageResponse<UserAdminResponse> adminList(UserListRequest request);

    UserAdminResponse adminDetail(Long id);

    boolean updateEnabled(Long id, Boolean enabled);

    boolean replaceRoles(Long id, List<Long> roleIds);
}
