package com.example.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.common.orm.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("operation_log")
public class OperationLog extends BaseEntity {
    private Long userId;
    private String username;
    private String action;
    private String targetType;
    private String targetId;
    private String detail;
    private String ip;
    private String result;
}
