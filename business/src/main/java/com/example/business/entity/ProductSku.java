package com.example.business.entity;

import com.baomidou.mybatisplus.annotation.Version;
import com.example.common.orm.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
public class ProductSku extends BaseEntity {
    /**
     * 名称
     */
    private String title;

    /**
     * 描述
     */
    private String description;

    /**
     * 价格
     */
    private BigDecimal price;

    /**
     * 可选展示原价，存在时必须高于成交价。
     */
    private BigDecimal originalPrice;

    /**
     * 库存
     */
    private Integer stock;

    /**
     * 管理端更新使用的乐观锁版本。
     */
    @Version
    private Integer version;

    /**
     * 商品内唯一的 SKU 编码。
     */
    private String skuCode;

    /**
     * SKU 主图记录 ID，由商品图片任务填充。
     */
    private Long mainImageId;

    /**
     * 商品ID
     */
    private Long productId;

}
