drop table if exists `product_sku`;
create table if not exists `product_sku`
(
    `id`          bigint         not null comment 'ID',
    `title`       varchar(255)   not null comment '名称｜searchable',
    `description` varchar(255)   null comment '描述',
    `price`       decimal(10, 2) not null comment '价格',
    `stock`       int            not null comment '库存',
    `product_id`  bigint         not null comment '商品ID',

    -- 通用基础字段
    `created_at`  datetime default null comment '创建时间',
    `created_by`  bigint   default null comment '创建人ID',
    `updated_at`  datetime default null comment '更新时间',
    `updated_by`  bigint   default null comment '修改人ID',
    `deleted`     bigint   default 0 comment '逻辑删除标记',
    `deleted_at`  datetime default null comment '删除时间',
    `deleted_by`  bigint   default null comment '删除人ID',

    primary key (`id`),
    key `idx_title` (`title`)
    ) comment '商品SKU';