drop table if exists `address`;
create table if not exists `address`
(
    `id`         bigint comment 'id',
    `address`    varchar(100) not null comment '地址｜searchable',
    `user_id`    bigint       not null comment '用户id',

    -- 通用基础字段
    `created_at` datetime default null comment '创建时间',
    `created_by` bigint   default null comment '创建人ID',
    `updated_at` datetime default null comment '更新时间',
    `updated_by` bigint   default null comment '修改人ID',
    `deleted`    bigint   default 0 comment '逻辑删除标记',
    `deleted_at` datetime default null comment '删除时间',
    `deleted_by` bigint   default null comment '删除人ID',

    primary key (`id`),
    unique key (`address`)
    ) comment '收货地址';
