alter table `address`
    modify column `address` varchar(255) null comment '地址｜searchable',
    add column `recipient` varchar(50) default null comment '收件人',
    add column `phone` varchar(20) default null comment '手机号',
    add column `province` varchar(50) default null comment '省',
    add column `city` varchar(50) default null comment '市',
    add column `district` varchar(50) default null comment '区/县',
    add column `detail` varchar(255) default null comment '详细地址',
    add column `is_default` tinyint not null default 0 comment '是否默认地址';

update `address`
set `detail` = `address`
where `detail` is null
  and `address` is not null;

alter table `address`
    drop index `uk_address_deleted`,
    add index `idx_address_user_default` (`user_id`, `is_default`, `deleted`);

-- rollback:
-- alter table `address`
--     drop index `idx_address_user_default`,
--     add unique key `uk_address_deleted` (`address`, `deleted`),
--     drop column `is_default`,
--     drop column `detail`,
--     drop column `district`,
--     drop column `city`,
--     drop column `province`,
--     drop column `phone`,
--     drop column `recipient`,
--     modify column `address` varchar(100) not null comment '地址｜searchable';
