create table "PlaylistTrack"
(
    "PlaylistId" integer not null
        constraint "FK_PlaylistTrackPlaylistId"
            references "Playlist",
    "TrackId"    integer not null
        constraint "FK_PlaylistTrackTrackId"
            references "Track",
    constraint "PK_PlaylistTrack"
        primary key ("PlaylistId", "TrackId")
);

alter table "PlaylistTrack"
    owner to postgres;

create index "IFK_PlaylistTrackTrackId"
    on "PlaylistTrack" ("TrackId");

