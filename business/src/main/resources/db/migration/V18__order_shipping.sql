alter table `order`
    add column shipping_carrier varchar(50) null comment '物流公司' after remark,
    add column tracking_number varchar(100) null comment '运单号' after shipping_carrier,
    add column shipping_note varchar(255) null comment '发货备注' after tracking_number;
