package com.example.business.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
/**
 * Product.price is the display/start price; transaction pricing comes from the selected SKU.
 */


@Getter
@Setter
public class ProductDetailResponse {
    private ProductResponse product;
    private List<AttributeResponse> attributes = new ArrayList<>();
    private List<SkuResponse> skus = new ArrayList<>();
    private List<ImageResponse> images = new ArrayList<>();

    @Getter
    @Setter
    public static class AttributeResponse {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        private String name;
        private List<AttributeValueResponse> values = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class AttributeValueResponse {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        private String value;
    }

    @Getter
    @Setter
    public static class SkuResponse {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        private BigDecimal price;
        private BigDecimal originalPrice;
        private Integer stock;
        private String skuCode;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long mainImageId;
        @JsonSerialize(contentUsing = ToStringSerializer.class)
        private List<Long> attributeValueIds = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class ImageResponse {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long skuId;
        private String originalUrl;
        private String styledUrl;
        private String url;
        private String status;
        private String stylePreset;
        private Boolean isMain;
    }
}
