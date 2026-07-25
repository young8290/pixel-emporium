-- rollback:
-- alter table `product_sku` drop column `version`;

alter table `product_sku`
    add column `version` int not null default 0 comment '乐观锁版本';
