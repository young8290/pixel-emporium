create table if not exists `order`
(
    `id`                bigint         not null comment 'ID',
    `order_no`          varchar(32)    not null comment '订单号',
    `user_id`           bigint         not null comment '用户ID',
    `status`            varchar(20)    not null comment '订单状态',
    `total_amount`      decimal(10, 2) not null comment '商品总金额',
    `pay_amount`        decimal(10, 2) not null comment '应付金额',
    `freight_amount`    decimal(10, 2) not null default 0.00 comment '运费',
    `receiver_name`     varchar(50)    not null comment '收件人',
    `receiver_phone`    varchar(20)    not null comment '收件人手机号',
    `receiver_province` varchar(50)    not null comment '省',
    `receiver_city`     varchar(50)    not null comment '市',
    `receiver_district` varchar(50)    not null comment '区县',
    `receiver_detail`   varchar(255)   not null comment '详细地址',
    `remark`            varchar(255)   null comment '订单备注',
    `paid_at`           datetime       default null comment '支付时间',
    `shipped_at`        datetime       default null comment '发货时间',
    `completed_at`      datetime       default null comment '完成时间',
    `canceled_at`       datetime       default null comment '取消时间',
    `created_at`        datetime       default null comment '创建时间',
    `created_by`        bigint         default null comment '创建人ID',
    `updated_at`        datetime       default null comment '更新时间',
    `updated_by`        bigint         default null comment '修改人ID',
    `deleted`           bigint         not null default 0 comment '逻辑删除标记',
    `deleted_at`        datetime       default null comment '删除时间',
    `deleted_by`        bigint         default null comment '删除人ID',

    primary key (`id`),
    unique key `uk_order_no` (`order_no`),
    key `idx_user_status` (`user_id`, `status`)
) comment '订单';

create table if not exists `order_item`
(
    `id`             bigint         not null comment 'ID',
    `order_id`       bigint         not null comment '订单ID',
    `product_id`     bigint         not null comment '商品ID',
    `product_sku_id` bigint         not null comment 'SKU ID',
    `product_title`  varchar(255)   not null comment '商品标题快照',
    `sku_title`      varchar(255)   not null comment 'SKU标题快照',
    `image`          varchar(255)   null comment '商品主图快照',
    `price`          decimal(10, 2) not null comment '单价快照',
    `quantity`       int            not null comment '数量',
    `created_at`     datetime       default null comment '创建时间',
    `created_by`     bigint         default null comment '创建人ID',
    `updated_at`     datetime       default null comment '更新时间',
    `updated_by`     bigint         default null comment '修改人ID',
    `deleted`        bigint         not null default 0 comment '逻辑删除标记',
    `deleted_at`     datetime       default null comment '删除时间',
    `deleted_by`     bigint         default null comment '删除人ID',

    primary key (`id`),
    key `idx_order` (`order_id`)
) comment '订单项';

-- rollback:
-- drop table if exists `order_item`;
-- drop table if exists `order`;
