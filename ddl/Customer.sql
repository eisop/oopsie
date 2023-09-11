create table "Customer"
(
    "CustomerId"   integer     not null
        constraint "PK_Customer"
            primary key,
    "FirstName"    varchar(40) not null,
    "LastName"     varchar(20) not null,
    "Company"      varchar(80),
    "Address"      varchar(70),
    "City"         varchar(40),
    "State"        varchar(40),
    "Country"      varchar(40),
    "PostalCode"   varchar(10),
    "Phone"        varchar(24),
    "Fax"          varchar(24),
    "Email"        varchar(60) not null,
    "SupportRepId" integer
        constraint "FK_CustomerSupportRepId"
            references "Employee"
);

alter table "Customer"
    owner to postgres;

create index "IFK_CustomerSupportRepId"
    on "Customer" ("SupportRepId");

