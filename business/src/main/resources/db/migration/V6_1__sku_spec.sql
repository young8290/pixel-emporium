create table if not exists `product_attribute`
(
    `id`         bigint       not null comment 'ID',
    `product_id` bigint       not null comment '商品ID',
    `name`       varchar(64)  not null comment '规格名称',
    `sort`       int          not null default 99 comment '排序值',
    `created_at` datetime      default null comment '创建时间',
    `created_by` bigint        default null comment '创建人ID',
    `updated_at` datetime      default null comment '更新时间',
    `updated_by` bigint        default null comment '修改人ID',
    `deleted`    bigint       not null default 0 comment '逻辑删除标记',
    `deleted_at` datetime      default null comment '删除时间',
    `deleted_by` bigint        default null comment '删除人ID',
    primary key (`id`),
    key `idx_pa_product_deleted` (`product_id`, `deleted`)
) comment '商品规格属性';

create table if not exists `product_attribute_value`
(
    `id`           bigint       not null comment 'ID',
    `product_id`   bigint       not null comment '商品ID',
    `attribute_id` bigint       not null comment '规格属性ID',
    `value`        varchar(128) not null comment '规格值',
    `sort`         int          not null default 99 comment '排序值',
    `created_at`   datetime      default null comment '创建时间',
    `created_by`   bigint        default null comment '创建人ID',
    `updated_at`   datetime      default null comment '更新时间',
    `updated_by`   bigint        default null comment '修改人ID',
    `deleted`      bigint       not null default 0 comment '逻辑删除标记',
    `deleted_at`   datetime      default null comment '删除时间',
    `deleted_by`   bigint        default null comment '删除人ID',
    primary key (`id`),
    key `idx_pav_attribute_deleted` (`attribute_id`, `deleted`),
    key `idx_pav_product_deleted` (`product_id`, `deleted`)
) comment '商品规格属性值';

alter table `product_sku`
    add column `sku_code` varchar(64) default null comment '商品内SKU编码',
    add column `main_image_id` bigint default null comment 'SKU主图ID',
    add unique key `uk_product_skucode` (`product_id`, `sku_code`, `deleted`);

create table if not exists `sku_attribute_value`
(
    `id`                 bigint   not null comment 'ID',
    `product_id`         bigint   not null comment '商品ID',
    `sku_id`             bigint   not null comment 'SKU ID',
    `attribute_id`       bigint   not null comment '规格属性ID',
    `attribute_value_id` bigint   not null comment '规格值ID',
    `created_at`         datetime default null comment '创建时间',
    `created_by`         bigint   default null comment '创建人ID',
    `updated_at`         datetime default null comment '更新时间',
    `updated_by`         bigint   default null comment '修改人ID',
    `deleted`            bigint   not null default 0 comment '逻辑删除标记',
    `deleted_at`         datetime default null comment '删除时间',
    `deleted_by`         bigint   default null comment '删除人ID',
    primary key (`id`),
    unique key `uk_sku_attr` (`sku_id`, `attribute_id`),
    key `idx_sav_product` (`product_id`),
    key `idx_sav_sku` (`sku_id`)
) comment 'SKU规格值映射';

-- rollback:
-- drop table if exists `sku_attribute_value`;
-- alter table `product_sku` drop index `uk_product_skucode`;
-- alter table `product_sku` drop column `main_image_id`, drop column `sku_code`;
-- drop table if exists `product_attribute_value`;
-- drop table if exists `product_attribute`;
