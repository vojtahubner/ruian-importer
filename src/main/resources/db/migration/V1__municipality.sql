-- Municipalities (obce) and their parts (části obce) imported from RÚIAN.
create table municipality (
    code       integer primary key,
    name       varchar(48) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table municipality_part (
    code              integer primary key,
    name              varchar(48) not null,
    municipality_code integer not null references municipality (code) on delete cascade,
    created_at        timestamptz not null,
    updated_at        timestamptz not null
);

create index municipality_part_municipality_code_idx on municipality_part (municipality_code);
