alter table mail_delivery add column claimed_at datetime null after next_retry_at;

create index idx_mail_delivery_claimed_at on mail_delivery (status, claimed_at);
