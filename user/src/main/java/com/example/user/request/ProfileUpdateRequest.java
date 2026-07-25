package com.example.user.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileUpdateRequest {
    @NotBlank(message = "昵称不能为空")
    @Size(max = 255, message = "昵称长度不能超过 {max} 位")
    private String name;

    @Size(max = 20, message = "手机号长度不能超过 {max} 位")
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String mobile;

    @Size(max = 512, message = "头像地址长度不能超过 {max} 位")
    private String avatar;
}
