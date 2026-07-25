package com.example.business.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ProductSkuRequest {
    private Long id;

    @NotBlank(message = "SKU名称不能为空")
    @Size(max = 255, message = "SKU名称不能超过255个字符")
    private String title;

    @Size(max = 255, message = "描述不能超过255个字符")
    private String description;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于0")
    @Digits(integer = 8, fraction = 2, message = "价格最多为八位整数和两位小数")
    private BigDecimal price;

    @DecimalMin(value = "0.01", message = "原价必须大于0")
    @Digits(integer = 8, fraction = 2, message = "原价最多为八位整数和两位小数")
    private BigDecimal originalPrice;

    @NotNull(message = "库存不能为空")
    @PositiveOrZero(message = "库存不能小于0")
    private Integer stock;

    /**
     * 管理端更新时携带的乐观锁版本。
     */
    private Integer version;

    /**
     * 管理员调整库存时写入库存流水的原因。
     */
    @Size(max = 255, message = "库存调整原因不能超过255个字符")
    private String stockAdjustReason;

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @Size(max = 64, message = "SKU code must not exceed 64 characters")
    private String skuCode;

    private Long mainImageId;

    private List<Long> attributeValueIds = new ArrayList<>();
}
