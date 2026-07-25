create table if not exists `payment`
(
    `id`         bigint         not null comment 'ID',
    `order_id`   bigint         not null comment '订单ID',
    `order_no`   varchar(32)    not null comment '订单号快照',
    `pay_no`     varchar(32)    not null comment '支付流水号',
    `amount`     decimal(10, 2) not null comment '支付金额',
    `channel`    varchar(20)    not null default 'MOCK' comment '支付渠道',
    `status`     varchar(20)    not null comment '支付状态',
    `paid_at`    datetime       default null comment '支付成功时间',
    `created_at` datetime       default null comment '创建时间',
    `created_by` bigint         default null comment '创建人ID',
    `updated_at` datetime       default null comment '更新时间',
    `updated_by` bigint         default null comment '修改人ID',
    `deleted`    bigint         not null default 0 comment '逻辑删除标记',
    `deleted_at` datetime       default null comment '删除时间',
    `deleted_by` bigint         default null comment '删除人ID',

    primary key (`id`),
    unique key `uk_payment_pay_no` (`pay_no`),
    unique key `uk_payment_order` (`order_id`),
    key `idx_order` (`order_id`)
) comment '支付记录';

-- rollback:
-- drop table if exists `payment`;
