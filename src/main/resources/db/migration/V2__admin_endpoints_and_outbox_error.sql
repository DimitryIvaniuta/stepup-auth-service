-- Adds operational/admin support columns and improves outbox diagnostics.

alter table if exists outbox_event
    add column if not exists last_error text null;

-- Helpful indexes for admin queries.
create index if not exists idx_step_up_challenge_user_created on step_up_challenge(user_id, created_at desc);
create index if not exists idx_outbox_created_at on outbox_event(created_at desc);
create index if not exists idx_outbox_event_type_created on outbox_event(event_type, created_at desc);
