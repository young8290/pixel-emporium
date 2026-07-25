package com.example.business.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.business.enums.PaymentStatus;
import com.example.common.orm.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("payment")
public class Payment extends BaseEntity {
    private Long orderId;
    private String orderNo;
    private String payNo;
    private String tradeNo;
    private BigDecimal amount;
    private String channel;
    private PaymentStatus status;
    private LocalDateTime paidAt;
    private LocalDateTime notifiedAt;
    private String notificationSummary;
}
