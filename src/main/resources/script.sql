create table omg (
    id bigserial primary key,
    info json
);

create table master(
    id bigserial primary key,
    description varchar(255)
);

create table detail(
    id bigserial primary key,
    master_id bigint references master(id),
    description varchar(255)
);