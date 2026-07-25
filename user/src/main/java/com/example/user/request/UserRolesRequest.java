package com.example.user.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UserRolesRequest {
    @NotEmpty(message = "roleIds 不能为空")
    private List<@NotNull(message = "角色ID不能为空") Long> roleIds;
}
