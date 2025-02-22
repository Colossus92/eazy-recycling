create table IF NOT EXISTS trucks (
                        updated_at timestamp(6) not null,
                        brand varchar(255),
                        license_plate varchar(255) not null,
                        model varchar(255),
                        primary key (license_plate)
);