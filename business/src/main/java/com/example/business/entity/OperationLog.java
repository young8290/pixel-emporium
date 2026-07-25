package com.example.business.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.common.orm.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 操作审计日志（BE-P2-01）。
 * 记录管理员关键操作（登录、发货、改角色、删除、库存调整）与订单状态变更。
 */
@Getter
@Setter
@TableName("operation_log")
public class OperationLog extends BaseEntity {
    /** 操作人ID */
    private Long userId;
    /** 操作人用户名 */
    private String username;
    /** 操作动作，如 ORDER_SHIP / USER_ROLE_UPDATE */
    private String action;
    /** 目标类型，如 ORDER / USER / PRODUCT */
    private String targetType;
    /** 目标ID */
    private String targetId;
    /** 操作详情（已脱敏） */
    private String detail;
    /** 操作来源IP */
    private String ip;
    /** 结果 SUCCESS / FAIL */
    private String result;
}
