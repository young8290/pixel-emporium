alter table user
    add column token_version bigint not null default 0 after enabled;
