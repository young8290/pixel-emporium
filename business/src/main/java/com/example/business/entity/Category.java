package com.example.business.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.common.orm.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("category")
public class Category extends BaseEntity {
    private String name;
    private Long parentId;
    private Integer sortOrder;
    private String icon;
    private Integer enabled;
}
