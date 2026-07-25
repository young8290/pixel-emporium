create table if not exists `product_image`
(
    `id`           bigint       not null comment 'ID',
    `product_id`   bigint       not null comment '商品ID',
    `sku_id`       bigint       default null comment 'SKU ID，可空',
    `original_url` varchar(1024) not null comment '原图地址',
    `styled_url`   varchar(1024) default null comment '风格图地址',
    `style_status` varchar(16)  not null default 'NONE' comment '风格转换状态',
    `style_preset` varchar(64)  default null comment '风格预设',
    `sort`         int          not null default 99 comment '排序值',
    `is_main`      tinyint      not null default 0 comment '是否主图',
    `created_at`   datetime     default null comment '创建时间',
    `created_by`   bigint       default null comment '创建人ID',
    `updated_at`   datetime     default null comment '更新时间',
    `updated_by`   bigint       default null comment '修改人ID',
    `deleted`      bigint       not null default 0 comment '逻辑删除标记',
    `deleted_at`   datetime     default null comment '删除时间',
    `deleted_by`   bigint       default null comment '删除人ID',
    primary key (`id`),
    key `idx_pi_product_deleted` (`product_id`, `deleted`),
    key `idx_pi_sku` (`sku_id`)
) comment '商品图片';

-- rollback:
-- drop table if exists `product_image`;
