create table if not exists mail_config
(
    id bigint not null, enabled tinyint not null default 0, host varchar(255) null, port int null,
    security_mode varchar(20) not null default 'STARTTLS', username varchar(255) null,
    password_ciphertext varchar(2048) null, from_address varchar(255) null, from_name varchar(100) null,
    reply_to varchar(255) null, connection_timeout_ms int not null default 5000,
    read_timeout_ms int not null default 10000, version int not null default 0,
    created_at datetime null, created_by bigint null, updated_at datetime null, updated_by bigint null,
    deleted bigint not null default 0, deleted_at datetime null, deleted_by bigint null, primary key (id)
) comment 'dynamic SMTP configuration';

create table if not exists mail_delivery
(
    id bigint not null, event_id varchar(128) not null, template_key varchar(40) not null,
    recipient varchar(255) null, subject varchar(255) not null, model_ciphertext longtext null,
    status varchar(20) not null, attempts int not null default 0, max_attempts int not null default 6,
    next_retry_at datetime null, sent_at datetime null, last_error varchar(512) null,
    created_at datetime null, created_by bigint null, updated_at datetime null, updated_by bigint null,
    deleted bigint not null default 0, deleted_at datetime null, deleted_by bigint null, primary key (id),
    unique key uk_mail_delivery_event (event_id), key idx_mail_delivery_due (status, next_retry_at),
    key idx_mail_delivery_recipient (recipient, created_at)
) comment 'mail delivery records';

insert into mail_config (id, enabled, security_mode, connection_timeout_ms, read_timeout_ms, version, created_at, updated_at, deleted)
values (1, 0, 'STARTTLS', 5000, 10000, 0, now(), now(), 0)
on duplicate key update id = 1;

-- rollback documentation only: drop table mail_delivery; drop table mail_config;
