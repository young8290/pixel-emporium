create table if not exists `product_favorite`
(
    `id`         bigint   not null comment 'ID',
    `user_id`    bigint   not null comment '用户ID',
    `product_id` bigint   not null comment '商品ID',

    `created_at` datetime default null comment '创建时间',
    `created_by` bigint   default null comment '创建人ID',
    `updated_at` datetime default null comment '更新时间',
    `updated_by` bigint   default null comment '修改人ID',
    `deleted`    bigint   default 0 comment '逻辑删除标记',
    `deleted_at` datetime default null comment '删除时间',
    `deleted_by` bigint   default null comment '删除人ID',

    primary key (`id`),
    unique key `uk_favorite_user_product_deleted` (`user_id`, `product_id`, `deleted`),
    key `idx_favorite_user_created` (`user_id`, `created_at`, `id`)
) comment '商品收藏';

create table if not exists `user_browse_history`
(
    `id`             bigint   not null comment 'ID',
    `user_id`        bigint   not null comment '用户ID',
    `product_id`     bigint   not null comment '商品ID',
    `view_count`     int      not null default 1 comment '浏览次数',
    `last_viewed_at` datetime not null comment '最近浏览时间',

    `created_at`     datetime default null comment '创建时间',
    `created_by`     bigint   default null comment '创建人ID',
    `updated_at`     datetime default null comment '更新时间',
    `updated_by`     bigint   default null comment '修改人ID',
    `deleted`        bigint   default 0 comment '逻辑删除标记',
    `deleted_at`     datetime default null comment '删除时间',
    `deleted_by`     bigint   default null comment '删除人ID',

    primary key (`id`),
    unique key `uk_history_user_product_deleted` (`user_id`, `product_id`, `deleted`),
    key `idx_history_user_viewed` (`user_id`, `last_viewed_at`, `id`),
    constraint `chk_history_view_count` check (`view_count` > 0)
) comment '商品浏览历史';

-- rollback:
-- drop table if exists `user_browse_history`;
-- drop table if exists `product_favorite`;
