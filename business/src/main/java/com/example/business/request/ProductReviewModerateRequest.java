package com.example.business.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductReviewModerateRequest {
    @NotBlank(message = "隐藏原因不能为空")
    @Size(min = 2, max = 200, message = "隐藏原因长度应为2到200个字符")
    private String reason;
}
