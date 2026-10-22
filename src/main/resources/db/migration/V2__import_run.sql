-- Log of import runs: one row per attempt, whether it succeeded or failed.
create table import_run (
    id                bigint generated always as identity primary key,
    source_url        varchar(2048) not null,
    status            varchar(16) not null check (status in ('SUCCEEDED', 'FAILED')),
    started_at        timestamptz not null,
    finished_at       timestamptz not null,
    -- No foreign key on purpose: a failed run has no municipality.
    municipality_code integer,
    part_count        integer not null default 0,
    error_message     varchar(1000),
    check ((status = 'FAILED') = (error_message is not null))
);

create index import_run_started_at_idx on import_run (started_at desc);
