create table if not exists `cart`
(
    `id`             bigint   not null comment 'ID',
    `user_id`        bigint   not null comment '用户ID',
    `product_sku_id` bigint   not null comment '商品SKU ID',
    `quantity`       int      not null comment '数量',
    `checked`        tinyint  not null default 1 comment '是否勾选',

    `created_at`     datetime default null comment '创建时间',
    `created_by`     bigint   default null comment '创建人ID',
    `updated_at`     datetime default null comment '更新时间',
    `updated_by`     bigint   default null comment '修改人ID',
    `deleted`        bigint   not null default 0 comment '逻辑删除标记',
    `deleted_at`     datetime default null comment '删除时间',
    `deleted_by`     bigint   default null comment '删除人ID',

    primary key (`id`),
    unique key `uk_cart_user_sku_deleted` (`user_id`, `product_sku_id`, `deleted`),
    key `idx_user` (`user_id`)
) comment '用户购物车';

-- rollback:
-- drop table if exists `cart`;
