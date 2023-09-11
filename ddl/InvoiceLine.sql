create table "InvoiceLine"
(
    "InvoiceLineId" integer        not null
        constraint "PK_InvoiceLine"
            primary key,
    "InvoiceId"     integer        not null
        constraint "FK_InvoiceLineInvoiceId"
            references invoice,
    "TrackId"       integer        not null
        constraint "FK_InvoiceLineTrackId"
            references "Track",
    "UnitPrice"     numeric(10, 2) not null,
    "Quantity"      integer        not null
);

alter table "InvoiceLine"
    owner to postgres;

create index "IFK_InvoiceLineInvoiceId"
    on "InvoiceLine" ("InvoiceId");

create index "IFK_InvoiceLineTrackId"
    on "InvoiceLine" ("TrackId");

