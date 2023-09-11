create table "Playlist"
(
    "PlaylistId" integer not null
        constraint "PK_Playlist"
            primary key,
    "Name"       varchar(120)
);

alter table "Playlist"
    owner to postgres;

