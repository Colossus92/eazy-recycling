CREATE TABLE IF NOT EXISTS trucks (
                        updated_at timestamp(6) not null,
                        brand varchar(255),
                        license_plate varchar(255) not null,
                        model varchar(255),
                        primary key (license_plate)
);

CREATE TABLE IF NOT EXISTS companies (
                           updated_at timestamp(6),
                           id uuid not null,
                           building_name varchar(255),
                           building_number varchar(255),
                           chamber_of_commerce_id varchar(255) unique,
                           city varchar(255),
                           country varchar(255),
                           name varchar(255),
                           postal_code varchar(255),
                           street_name varchar(255),
                           vihb_id varchar(255) unique,
                           primary key (id)
);
