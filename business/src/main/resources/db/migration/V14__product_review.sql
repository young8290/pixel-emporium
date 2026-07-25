create table if not exists product_review
(
    id                bigint        not null,
    product_id        bigint        not null,
    order_id          bigint        not null,
    order_item_id     bigint        not null,
    user_id           bigint        not null,
    rating            tinyint       not null,
    content           varchar(1000) not null,
    status            varchar(20)   not null default 'PUBLISHED',
    moderation_reason varchar(200)  null,
    moderated_at      datetime      null,
    moderated_by      bigint        null,
    created_at        datetime      null,
    created_by        bigint        null,
    updated_at        datetime      null,
    updated_by        bigint        null,
    deleted           bigint        not null default 0,
    deleted_at        datetime      null,
    deleted_by        bigint        null,
    primary key (id),
    unique key uk_review_order_item (order_item_id, deleted),
    key idx_review_product_public (product_id, status, created_at, id),
    key idx_review_user (user_id, created_at, id),
    key idx_review_order (order_id, order_item_id),
    constraint chk_review_rating check (rating between 1 and 5)
) comment '商品评价';

alter table product
    alter column rating set default 0.00,
    alter column review_count set default 0;

update product set rating = 0.00, review_count = 0 where deleted = 0;
