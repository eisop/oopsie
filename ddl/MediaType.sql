create table "MediaType"
(
    "MediaTypeId" integer not null
        constraint "PK_MediaType"
            primary key,
    "Name"        varchar(120)
);

alter table "MediaType"
    owner to postgres;

