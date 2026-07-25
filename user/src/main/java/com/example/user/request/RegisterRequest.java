package com.example.user.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 255, message = "邮箱长度不能超过 {max} 位")
    private String email;

    @NotBlank(message = "邮箱验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "邮箱验证码必须为6位数字")
    private String emailCode;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "用户名不能为空")
    @Size(max = 255, message = "用户名长度不能超过 {max} 位")
    private String name;

    @Size(max = 20, message = "手机号长度不能超过 {max} 位")
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String mobile;
}
