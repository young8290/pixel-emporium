package com.example.business.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.common.orm.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("product_attribute_value")
public class ProductAttributeValue extends BaseEntity {
    private Long productId;
    private Long attributeId;
    private String value;
    private Integer sort;
}
