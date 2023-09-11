create table "Artist"
(
    "ArtistId" integer not null
        constraint "PK_Artist"
            primary key,
    "Name"     varchar(120)
);

alter table "Artist"
    owner to postgres;

