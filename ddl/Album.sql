create table "Album"
(
    "AlbumId"  integer      not null
        constraint "PK_Album"
            primary key,
    "Title"    varchar(160) not null,
    "ArtistId" integer      not null
        constraint "FK_AlbumArtistId"
            references "Artist"
);

alter table "Album"
    owner to postgres;

create index "IFK_AlbumArtistId"
    on "Album" ("ArtistId");

