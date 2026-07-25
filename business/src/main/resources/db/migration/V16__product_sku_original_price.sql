alter table `product_sku`
    add column `original_price` decimal(10, 2) null comment '展示原价' after `price`,
    add constraint `chk_product_sku_original_price`
        check (`original_price` is null or `original_price` > `price`);

-- rollback:
-- alter table `product_sku` drop check `chk_product_sku_original_price`;
-- alter table `product_sku` drop column `original_price`;
