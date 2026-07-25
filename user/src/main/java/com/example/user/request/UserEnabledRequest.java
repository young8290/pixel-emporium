package com.example.user.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserEnabledRequest {
    @NotNull(message = "enabled 不能为空")
    private Boolean enabled;
}
