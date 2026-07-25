drop table if exists `product`;
create table if not exists `product`
(
    `id`           bigint                  not null comment 'ID',
    `title`        varchar(255)            not null comment '名称｜searchable',
    `description`  varchar(512)            null comment '详情',
    `image`        varchar(255)            null comment '封面图｜imageColumn',
    `on_sale`      tinyint        default 1    not null comment '上架状态',
    `rating`       decimal(4, 2)  default 5.00 null comment '平均评分',
    `sold_count`   int            default 0    null comment '销量',
    `review_count` int            default 0    null comment '评价数量',
    `price`        decimal(10, 2) default 0.00 not null comment '最低价格',

    -- 通用基础字段
    `created_at`   datetime      default null comment '创建时间',
    `created_by`   bigint        default null comment '创建人ID',
    `updated_at`   datetime      default null comment '更新时间',
    `updated_by`   bigint        default null comment '修改人ID',
    `deleted`      bigint        default 0 comment '逻辑删除标记',
    `deleted_at`   datetime      default null comment '删除时间',
    `deleted_by`   bigint        default null comment '删除人ID',

    primary key (`id`),
    key `idx_title` (`title`)
    ) comment '商品';