package com.example.business.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.common.orm.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("product_image")
public class ProductImage extends BaseEntity {
    private Long productId;
    private Long skuId;
    private String originalUrl;
    private String styledUrl;
    private StyleStatus styleStatus = StyleStatus.NONE;
    private String stylePreset;
    private Integer sort = 99;
    private Boolean isMain = false;

    public enum StyleStatus {
        NONE, PENDING, DONE, FAILED
    }
}
