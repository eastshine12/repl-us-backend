create table users (
    id uuid primary key,
    display_name varchar(24) not null,
    avatar_url varchar(2048),
    is_guest boolean not null,
    created_at timestamp with time zone not null
);

create table rooms (
    id uuid primary key,
    name varchar(32) not null,
    owner_member_id uuid not null,
    max_members integer not null default 6,
    created_at timestamp with time zone not null
);

create table room_members (
    id uuid primary key,
    room_id uuid not null references rooms(id),
    user_id uuid not null references users(id),
    role varchar(16) not null,
    status varchar(16) not null,
    slot_index integer not null,
    joined_at timestamp with time zone not null,
    removed_at timestamp with time zone,
    constraint uk_room_members_room_user unique (room_id, user_id)
);

create index idx_room_members_user_status on room_members(user_id, status);
create index idx_room_members_room_status on room_members(room_id, status);

create table invite_links (
    code varchar(32) primary key,
    room_id uuid not null references rooms(id),
    created_by_member_id uuid not null references room_members(id),
    expires_at timestamp with time zone not null,
    max_uses integer,
    uses integer not null default 0,
    created_at timestamp with time zone not null
);

create index idx_invite_links_room_expires on invite_links(room_id, expires_at);

create table missions (
    id uuid primary key,
    room_id uuid not null references rooms(id),
    mission_date date not null,
    prompt varchar(80) not null,
    category varchar(32) not null,
    edit_count integer not null default 0,
    edited_by_member_id uuid references room_members(id),
    edited_at timestamp with time zone,
    created_at timestamp with time zone not null,
    constraint uk_missions_room_date unique (room_id, mission_date)
);

create index idx_missions_room_date on missions(room_id, mission_date);

create table video_assets (
    id uuid primary key,
    object_key varchar(512) not null,
    content_type varchar(64) not null,
    file_size_bytes bigint not null,
    duration_seconds integer not null,
    has_audio boolean not null,
    width integer,
    height integer,
    thumbnail_object_key varchar(512),
    created_at timestamp with time zone not null
);

create table mission_responses (
    id uuid primary key,
    room_id uuid not null references rooms(id),
    mission_id uuid not null references missions(id),
    member_id uuid not null references room_members(id),
    video_asset_id uuid not null references video_assets(id),
    status varchar(16) not null,
    created_at timestamp with time zone not null,
    deleted_at timestamp with time zone,
    constraint uk_mission_responses_mission_member unique (mission_id, member_id)
);

create index idx_mission_responses_mission_status on mission_responses(mission_id, status);

create table mission_release_states (
    mission_id uuid primary key references missions(id),
    all_submitted_at timestamp with time zone,
    release_scheduled_at timestamp with time zone,
    released_at timestamp with time zone,
    failed_at timestamp with time zone
);
