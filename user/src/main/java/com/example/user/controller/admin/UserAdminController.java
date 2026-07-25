package com.example.user.controller.admin;

import com.example.common.annotation.CustomRequireRole;
import com.example.common.response.PageResponse;
import com.example.common.response.R;
import com.example.user.annotation.OperationLog;
import com.example.user.request.UserEnabledRequest;
import com.example.user.request.UserListRequest;
import com.example.user.request.UserRolesRequest;
import com.example.user.response.UserAdminResponse;
import com.example.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("admin/user")
@CustomRequireRole({"ADMIN", "SUPER_ADMIN"})
public class UserAdminController {
    private final UserService userService;

    public UserAdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/list")
    public R<PageResponse<UserAdminResponse>> list(@Valid UserListRequest request) {
        return R.ok(userService.adminList(request));
    }

    @GetMapping("/{id}")
    public R<UserAdminResponse> detail(@PathVariable Long id) {
        return R.ok(userService.adminDetail(id));
    }

    @PutMapping("/{id}/enabled")
    @OperationLog(action = "USER_ENABLED_UPDATE", targetType = "USER", targetIdSpEL = "#id")
    public R<Boolean> updateEnabled(@PathVariable Long id,
                                    @RequestBody @Valid UserEnabledRequest request) {
        return R.ok(userService.updateEnabled(id, request.getEnabled()));
    }

    @PutMapping("/{id}/roles")
    @CustomRequireRole("SUPER_ADMIN")
    @OperationLog(action = "USER_ROLES_UPDATE", targetType = "USER", targetIdSpEL = "#id")
    public R<Boolean> replaceRoles(@PathVariable Long id,
                                   @RequestBody @Valid UserRolesRequest request) {
        return R.ok(userService.replaceRoles(id, request.getRoleIds()));
    }
}
