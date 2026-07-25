package com.example.business.entity;

import com.example.common.orm.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Cart extends BaseEntity {
    private Long userId;

    private Long productSkuId;

    private Integer quantity;

    private Boolean checked;
}
