package com.example.user.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddressRequest {
    private Long id;

    @Size(max = 255, message = "地址长度不能超过 {max} 位")
    private String address;

    @NotBlank(message = "收件人不能为空")
    @Size(max = 50, message = "收件人长度不能超过 {max} 位")
    private String recipient;

    @NotBlank(message = "手机号不能为空")
    @Size(max = 20, message = "手机号长度不能超过 {max} 位")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "省不能为空")
    @Size(max = 50, message = "省长度不能超过 {max} 位")
    private String province;

    @NotBlank(message = "市不能为空")
    @Size(max = 50, message = "市长度不能超过 {max} 位")
    private String city;

    @NotBlank(message = "区县不能为空")
    @Size(max = 50, message = "区县长度不能超过 {max} 位")
    private String district;

    @NotBlank(message = "详细地址不能为空")
    @Size(max = 255, message = "详细地址长度不能超过 {max} 位")
    private String detail;

    private Boolean isDefault;

    private Long userId;
}
