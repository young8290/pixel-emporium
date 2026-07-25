package com.example.business.response;

import com.example.business.entity.ProductImage;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductImageResponse {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long productId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long skuId;
    private String originalUrl;
    private String styledUrl;
    private String url;
    private ProductImage.StyleStatus styleStatus;
    private String stylePreset;
    private Integer sort;
    private Boolean isMain;

    public static ProductImageResponse from(ProductImage image) {
        ProductImageResponse response = new ProductImageResponse();
        response.setId(image.getId());
        response.setProductId(image.getProductId());
        response.setSkuId(image.getSkuId());
        response.setOriginalUrl(image.getOriginalUrl());
        response.setStyledUrl(image.getStyledUrl());
        response.setUrl(preferredUrl(image));
        response.setStyleStatus(image.getStyleStatus());
        response.setStylePreset(image.getStylePreset());
        response.setSort(image.getSort());
        response.setIsMain(image.getIsMain());
        return response;
    }

    public static String preferredUrl(ProductImage image) {
        return image.getStyledUrl() == null || image.getStyledUrl().isBlank()
                ? image.getOriginalUrl() : image.getStyledUrl();
    }
}
