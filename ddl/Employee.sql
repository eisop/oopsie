create table "Employee"
(
    "EmployeeId" integer     not null
        constraint "PK_Employee"
            primary key,
    "LastName"   varchar(20) not null,
    "FirstName"  varchar(20) not null,
    "Title"      varchar(30),
    "ReportsTo"  integer
        constraint "FK_EmployeeReportsTo"
            references "Employee",
    "BirthDate"  timestamp,
    "HireDate"   timestamp,
    "Address"    varchar(70),
    "City"       varchar(40),
    "State"      varchar(40),
    "Country"    varchar(40),
    "PostalCode" varchar(10),
    "Phone"      varchar(24),
    "Fax"        varchar(24),
    "Email"      varchar(60)
);

alter table "Employee"
    owner to postgres;

create index "IFK_EmployeeReportsTo"
    on "Employee" ("ReportsTo");

