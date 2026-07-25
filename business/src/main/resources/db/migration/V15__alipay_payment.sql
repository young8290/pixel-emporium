alter table `payment`
    add column `trade_no` varchar(64) default null comment '支付宝交易号' after `pay_no`,
    add column `notified_at` datetime default null comment '最近有效通知时间' after `paid_at`,
    add column `notification_summary` varchar(255) default null comment '通知摘要' after `notified_at`,
    add unique key `uk_payment_trade_no` (`trade_no`);

-- rollback:
-- alter table `payment` drop index `uk_payment_trade_no`;
-- alter table `payment` drop column `notification_summary`;
-- alter table `payment` drop column `notified_at`;
-- alter table `payment` drop column `trade_no`;
