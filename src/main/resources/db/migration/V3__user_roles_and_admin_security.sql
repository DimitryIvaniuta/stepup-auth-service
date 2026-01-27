-- Add role support to users.
-- Roles are stored as a comma-separated string to keep the demo lightweight (e.g. 'USER,ADMIN').

alter table app_user
    add column if not exists roles varchar(200) not null default 'USER';

-- Backfill for existing rows (in case column existed without default in some environments).
update app_user set roles = 'USER' where roles is null or roles = '';
