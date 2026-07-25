package com.example.business.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.common.orm.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@TableName("product")
public class Product extends BaseEntity {
    /**
     * 名称
     */
    private String title;

    /**
     * 详情
     */
    private String description;

    /**
     * 封面图
     */
    private String image;

    /**
     * 上架状态
     */
    private Integer onSale;

    /**
     * 平均评分
     */
    private BigDecimal rating;

    /**
     * 销量
     */
    private Integer soldCount;

    /**
     * 评价数量
     */
    private Integer reviewCount;

    /**
     * 最低价格
     */
    private BigDecimal price;

    /**
     * 商品所属分类。
     */
    private Long categoryId;

}
