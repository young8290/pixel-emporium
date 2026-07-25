package com.example.business.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.common.orm.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 库存变动流水（BE-P2-03）。
 * 记录每次扣减/回滚/管理员调整，含 before/after 与幂等业务单号。
 */
@Getter
@Setter
@TableName("stock_log")
public class StockLog extends BaseEntity {
    /** 商品 SKU ID */
    private Long productSkuId;
    /** 变动数量（正=入库/回滚，负=扣减） */
    private Integer changeQty;
    /** 变动前库存 */
    private Integer beforeStock;
    /** 变动后库存 */
    private Integer afterStock;
    /** 类型 DEDUCT / RESTORE / ADMIN_ADJUST */
    private String type;
    /** 业务单号（订单号等，用于幂等） */
    private String bizNo;
    /** 操作人 ID */
    private Long operatorId;
    /** 备注（如调整原因） */
    private String remark;
}
