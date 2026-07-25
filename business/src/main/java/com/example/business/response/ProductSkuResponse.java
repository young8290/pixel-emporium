package com.example.business.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ProductSkuResponse {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String title;

    private String description;

    private BigDecimal price;

    private BigDecimal originalPrice;

    private Integer stock;

    private Integer version;

    private String skuCode;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long mainImageId;

    @JsonSerialize(contentUsing = ToStringSerializer.class)
    private List<Long> attributeValueIds = new ArrayList<>();

    @JsonSerialize(using = ToStringSerializer.class)
    private Long productId;

    private String productTitle;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

}
