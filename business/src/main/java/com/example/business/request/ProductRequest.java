package com.example.business.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductRequest {
    private Long id;

    @NotBlank(message = "商品名称不能为空")
    @Size(max = 255, message = "商品名称不能超过255个字符")
    private String title;

    @Size(max = 512, message = "商品详情不能超过512个字符")
    private String description;

    @Size(max = 255, message = "封面图地址不能超过255个字符")
    private String image;

    @NotNull(message = "请选择上架状态")
    private Boolean onSale;

    @NotNull(message = "商品价格不能为空")
    @DecimalMin(value = "0.01", message = "商品价格必须大于0")
    @Digits(integer = 8, fraction = 2, message = "商品价格最多为八位整数和两位小数")
    private BigDecimal price;

    private Long categoryId;
}
