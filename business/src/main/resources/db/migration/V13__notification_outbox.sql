create table if not exists notification_outbox
(
    id bigint not null, event_id varchar(128) not null, event_type varchar(40) not null,
    aggregate_id bigint not null, status varchar(20) not null, attempts int not null default 0,
    next_retry_at datetime null, claimed_at datetime null, delivered_at datetime null, last_error varchar(512) null,
    created_at datetime null, created_by bigint null, updated_at datetime null, updated_by bigint null,
    deleted bigint not null default 0, deleted_at datetime null, deleted_by bigint null, primary key (id),
    unique key uk_notification_event (event_id), key idx_notification_due (status, next_retry_at)
) comment 'reliable notification events';
