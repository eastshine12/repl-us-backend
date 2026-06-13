create table user_sessions (
    id uuid primary key,
    user_id uuid not null references users(id),
    token_hash varchar(64) not null,
    expires_at timestamp with time zone not null,
    created_at timestamp with time zone not null,
    revoked_at timestamp with time zone,
    constraint uk_user_sessions_token_hash unique (token_hash)
);

create index idx_user_sessions_user_expires on user_sessions(user_id, expires_at);
