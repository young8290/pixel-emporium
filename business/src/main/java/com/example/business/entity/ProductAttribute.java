package com.example.business.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.common.orm.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("product_attribute")
public class ProductAttribute extends BaseEntity {
    private Long productId;
    private String name;
    private Integer sort;
}
