alter table video_assets
    add column status varchar(32) not null default 'READY';

alter table video_assets
    add column uploaded_at timestamp with time zone;

update video_assets
set uploaded_at = created_at
where uploaded_at is null;

alter table video_assets
    alter column status drop default;

create unique index uk_video_assets_object_key on video_assets(object_key);
