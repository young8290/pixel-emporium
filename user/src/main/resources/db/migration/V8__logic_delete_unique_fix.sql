-- rollback:
-- alter table `user`
--     drop index `uk_email_deleted`,
--     drop index `uk_name_deleted`,
--     drop index `uk_phone_deleted`,
--     add unique key `uk_email` (`email`),
--     add unique key `uk_name` (`name`),
--     add unique key `uk_phone` (`mobile`);
-- alter table `address`
--     drop index `uk_address_deleted`,
--     add unique key `address` (`address`);

alter table `user`
    drop index `uk_email`,
    drop index `uk_name`,
    drop index `uk_phone`,
    add unique key `uk_email_deleted` (`email`, `deleted`),
    add unique key `uk_name_deleted` (`name`, `deleted`),
    add unique key `uk_phone_deleted` (`mobile`, `deleted`);

alter table `address`
    drop index `address`,
    add unique key `uk_address_deleted` (`address`, `deleted`);
