create table if not exists `stock_log`
(
    `id`             bigint       not null comment 'ID',
    `product_sku_id` bigint       not null comment '商品SKU ID',
    `change_qty`     int          not null comment '变动数量(正=入库/回滚, 负=扣减)',
    `before_stock`   int          not null comment '变动前库存',
    `after_stock`    int          not null comment '变动后库存',
    `type`           varchar(20)  not null comment '类型 DEDUCT/RESTORE/ADMIN_ADJUST',
    `biz_no`         varchar(64)  null comment '业务单号(订单号等，用于幂等)',
    `operator_id`    bigint       null comment '操作人ID',
    `remark`         varchar(255) null comment '备注(如调整原因)',

    `created_at`     datetime default null comment '创建时间',
    `created_by`     bigint   default null comment '创建人ID',
    `updated_at`     datetime default null comment '更新时间',
    `updated_by`     bigint   default null comment '修改人ID',
    `deleted`        bigint   not null default 0 comment '逻辑删除标记',
    `deleted_at`     datetime default null comment '删除时间',
    `deleted_by`     bigint   default null comment '删除人ID',

    primary key (`id`),
    key `idx_sku` (`product_sku_id`),
    key `idx_created_at` (`created_at`),
    unique key `uk_biz_sku_type` (`biz_no`, `product_sku_id`, `type`)
) comment '库存变动流水';

-- rollback:
-- drop table `stock_log`;
