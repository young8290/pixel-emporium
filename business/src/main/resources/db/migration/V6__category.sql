create table if not exists `category`
(
    `id`         bigint       not null comment 'ID',
    `name`       varchar(50)  not null comment '分类名称',
    `parent_id`  bigint       not null default 0 comment '父分类ID，0为顶级',
    `sort_order` int          not null default 99 comment '排序值',
    `icon`       varchar(255) null comment '图标文件名',
    `enabled`    tinyint      not null default 1 comment '启用状态',

    `created_at` datetime default null comment '创建时间',
    `created_by` bigint   default null comment '创建人ID',
    `updated_at` datetime default null comment '更新时间',
    `updated_by` bigint   default null comment '修改人ID',
    `deleted`    bigint   not null default 0 comment '逻辑删除标记',
    `deleted_at` datetime default null comment '删除时间',
    `deleted_by` bigint   default null comment '删除人ID',

    primary key (`id`),
    key `idx_parent` (`parent_id`)
) comment '商品分类';

alter table `product`
    add column `category_id` bigint default null comment '分类ID',
    add key `idx_category` (`category_id`);

insert into `category` (`id`, `name`, `parent_id`, `sort_order`, `icon`, `enabled`)
values
    (900001, '蔬菜',     0,  10, 'cat-vegetable.png', 1),
    (900011, '腌渍',     0,  20, 'cat-preserve.png',  1),
    (900002, '水果',     0,  30, 'cat-fruit.png',     1),
    (900012, '肉类',     0,  40, 'cat-meat.png',      1),
    (900003, '粮油',     0,  50, 'cat-grain.png',     1),
    (900013, '水产',     0,  60, 'cat-seafood.png',   1),
    (900004, '乳制品',   0,  70, 'cat-dairy.png',     1),
    (900014, '茶饮',     0,  80, 'cat-tea.png',       1),
    (900005, '烘焙',     0,  90, 'cat-bakery.png',    1),
    (900015, '坚果',     0, 100, 'cat-nuts.png',      1),
    (900006, '饮品',     0, 110, 'cat-drink.png',     1),
    (900016, '香草',     0, 120, 'cat-herb.png',      1),
    (900007, '手作',     0, 130, 'cat-handmade.png',  1),
    (900017, '菌菇',     0, 140, 'cat-mushroom.png',  1),
    (900008, '种子/园艺', 0, 150, 'cat-seed.png',      1),
    (900018, '蜂蜜',     0, 160, 'cat-honey.png',     1),
    (900009, '花卉',     0, 170, 'cat-flower.png',    1),
    (900019, '禽蛋',     0, 180, 'cat-egg.png',      1),
    (900010, '其他',     0, 190, 'cat-other.png',     1);

-- rollback:
-- delete from `category` where `id` between 900001 and 900019;
-- alter table `product` drop index `idx_category`, drop column `category_id`;
-- drop table `category`;
