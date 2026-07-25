package com.example.user.entity;

import com.example.common.orm.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Sb extends BaseEntity {
    /**
     * 姓名
     */
    private String name;

    /**
     * 地址
     */
    private String address;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 所属用户ID
     */
    private Long userId;
}
