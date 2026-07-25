package com.example.business.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.common.orm.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("sku_attribute_value")
public class SkuAttributeValue extends BaseEntity {
    private Long productId;
    private Long skuId;
    private Long attributeId;
    private Long attributeValueId;
}
