create table "Genre"
(
    "GenreId" integer not null
        constraint "PK_Genre"
            primary key,
    "Name"    varchar(120)
);

alter table "Genre"
    owner to postgres;

