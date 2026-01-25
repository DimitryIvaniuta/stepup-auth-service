create extension if not exists "uuid-ossp";

create table if not exists app_user (
    id uuid primary key,
    username varchar(100) not null unique,
    password_hash varchar(200) not null,
    created_at timestamptz not null default now()
);

create table if not exists trusted_device (
    id bigserial primary key,
    user_id uuid not null references app_user(id) on delete cascade,
    device_hash varchar(128) not null,
    first_seen_at timestamptz not null default now(),
    last_seen_at timestamptz not null default now(),
    unique(user_id, device_hash)
);
create index if not exists idx_trusted_device_user on trusted_device(user_id);

create table if not exists country_profile (
    user_id uuid primary key references app_user(id) on delete cascade,
    last_country varchar(2) null,
    updated_at timestamptz not null default now()
);

create table if not exists risk_decision (
    id uuid primary key,
    user_id uuid not null references app_user(id) on delete cascade,
    action_type varchar(50) not null,
    amount numeric(19,2) not null,
    device_hash varchar(128) not null,
    country varchar(2) not null,
    risk_score int not null,
    risk_level varchar(20) not null,
    decision varchar(20) not null,
    step_up_required boolean not null,
    step_up_challenge_id uuid null,
    created_at timestamptz not null default now()
);
create index if not exists idx_risk_decision_user_created on risk_decision(user_id, created_at desc);

create table if not exists step_up_challenge (
    id uuid primary key,
    user_id uuid not null references app_user(id) on delete cascade,
    decision_id uuid not null references risk_decision(id) on delete cascade,
    status varchar(20) not null,
    attempts int not null default 0,
    created_at timestamptz not null default now(),
    verified_at timestamptz null
);

create table if not exists outbox_event (
    id bigserial primary key,
    aggregate_id uuid not null,
    event_type varchar(100) not null,
    payload_json jsonb not null,
    status varchar(20) not null,
    attempts int not null default 0,
    next_attempt_at timestamptz not null default now(),
    created_at timestamptz not null default now(),
    published_at timestamptz null
);
create index if not exists idx_outbox_status_next_attempt on outbox_event(status, next_attempt_at);
