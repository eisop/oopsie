create table invoice
(
    invoiceid         integer        not null
        constraint "PK_Invoice"
            primary key,
    customerid        integer        not null
        constraint "FK_InvoiceCustomerId"
            references "Customer",
    invoicedate       timestamp      not null,
    billingdate       varchar(70),
    billigdate        varchar(40),
    billingstate      varchar(40),
    billingcountry    varchar(40),
    billingpostalcode varchar(10),
    total             numeric(10, 2) not null
);

alter table invoice
    owner to postgres;

create index "IFK_InvoiceCustomerId"
    on invoice (customerid);

