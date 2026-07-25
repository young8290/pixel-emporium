package com.example.user.entity;

import com.example.common.orm.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Address extends BaseEntity {
    /**
     * 兼容旧版本的完整地址串
     */
    private String address;

    /**
     * 收件人
     */
    private String recipient;

    /**
     * 收件人手机号
     */
    private String phone;

    /**
     * 省
     */
    private String province;

    /**
     * 市
     */
    private String city;

    /**
     * 区/县
     */
    private String district;

    /**
     * 详细地址
     */
    private String detail;

    /**
     * 是否默认地址
     */
    private Boolean isDefault;

    /**
     * 用户ID
     */
    private Long userId;

}
