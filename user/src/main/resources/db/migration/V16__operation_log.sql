create table if not exists `operation_log`
(
    `id`          bigint        not null comment 'ID',
    `user_id`     bigint        null comment '操作人ID',
    `username`    varchar(64)   null comment '操作人用户名',
    `action`      varchar(64)   not null comment '操作动作',
    `target_type` varchar(64)   null comment '目标类型',
    `target_id`   varchar(64)   null comment '目标ID',
    `detail`      varchar(1000) null comment '不含敏感信息的详情',
    `ip`          varchar(64)   null comment '操作来源IP',
    `result`      varchar(16)   not null default 'SUCCESS' comment '操作结果',
    `created_at`  datetime      default null comment '创建时间',
    `created_by`  bigint        default null comment '创建人ID',
    `updated_at`  datetime      default null comment '更新时间',
    `updated_by`  bigint        default null comment '修改人ID',
    `deleted`     bigint        not null default 0 comment '逻辑删除标记',
    `deleted_at`  datetime      default null comment '删除时间',
    `deleted_by`  bigint        default null comment '删除人ID',
    primary key (`id`),
    key `idx_user` (`user_id`),
    key `idx_action` (`action`),
    key `idx_target` (`target_type`, `target_id`),
    key `idx_created_at` (`created_at`)
) comment '用户与邮件管理操作审计日志';
