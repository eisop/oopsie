create table "Track"
(
    "TrackId"      integer        not null
        constraint "PK_Track"
            primary key,
    "Name"         varchar(200)   not null,
    "AlbumId"      integer
        constraint "FK_TrackAlbumId"
            references "Album",
    "MediaTypeId"  integer        not null
        constraint "FK_TrackMediaTypeId"
            references "MediaType",
    "GenreId"      integer
        constraint "FK_TrackGenreId"
            references "Genre",
    "Composer"     varchar(220),
    "Milliseconds" integer        not null,
    "Bytes"        integer,
    "UnitPrice"    numeric(10, 2) not null
);

alter table "Track"
    owner to postgres;

create index "IFK_TrackAlbumId"
    on "Track" ("AlbumId");

create index "IFK_TrackGenreId"
    on "Track" ("GenreId");

create index "IFK_TrackMediaTypeId"
    on "Track" ("MediaTypeId");

