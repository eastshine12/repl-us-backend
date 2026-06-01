create table response_reactions (
    id uuid primary key,
    response_id uuid not null references mission_responses(id),
    member_id uuid not null references room_members(id),
    type varchar(16) not null,
    created_at timestamp with time zone not null
);

create index idx_response_reactions_response on response_reactions(response_id);
create index idx_response_reactions_member on response_reactions(member_id);

create table response_comments (
    id uuid primary key,
    response_id uuid not null references mission_responses(id),
    member_id uuid not null references room_members(id),
    body varchar(80) not null,
    created_at timestamp with time zone not null,
    deleted_at timestamp with time zone
);

create index idx_response_comments_response_created on response_comments(response_id, created_at);
create index idx_response_comments_member on response_comments(member_id);
