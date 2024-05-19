FROM mysql:8

ENV MYSQL_DATABASE=Chinook \
    MYSQL_ROOT_PASSWORD=chinook

ADD Chinook_Mysql_schema.sql extras.sql /docker-entrypoint-initdb.d/

EXPOSE 3306