create table auth_provider_accounts (
    id uuid primary key,
    user_id uuid not null references users(id),
    provider varchar(16) not null,
    provider_subject varchar(191) not null,
    email varchar(320),
    display_name varchar(100),
    avatar_url varchar(2048),
    linked_at timestamp with time zone not null,
    last_login_at timestamp with time zone not null,
    constraint uk_auth_provider_accounts_provider_subject unique (provider, provider_subject)
);

create index idx_auth_provider_accounts_user on auth_provider_accounts(user_id);
