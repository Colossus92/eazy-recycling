create table if not exists companies (
                           id uuid not null,
                           code text,
                           building_number_addition text,
                           building_number text,
                           chamber_of_commerce_id text,
                           city text,
                           country text,
                           name text,
                           postal_code text,
                           street_name text,
                           processor_id text,
                           vihb_id text unique,
                           phone text,
                           email text,
                           is_supplier boolean not null default true,
                           is_customer boolean not null default true,
                           deleted_at timestamp with time zone,
                           deleted_by uuid,
                           created_at timestamp with time zone not null default now(),
                           created_by text,
                           last_modified_at timestamp with time zone not null default now(),
                           last_modified_by text,
                           primary key (id)
);

CREATE TABLE IF NOT EXISTS company_roles (
                           company_id UUID NOT NULL,
                           roles TEXT NOT NULL,
                           CONSTRAINT fk_company_roles_company_id FOREIGN KEY (company_id) REFERENCES companies(id)
);

create table if not exists company_project_locations (
                         id uuid not null,
                         company_id uuid not null,
                         street_name text not null,
                         postal_code text not null,
                         city text not null,
                         building_number_addition text,
                         building_number text not null,
                         country text,
                         created_at timestamp with time zone not null default now(),
                         updated_at timestamp with time zone,
                         primary key (id)
);


create table if not exists drivers (
                         updated_at timestamp(6) not null,
                         id uuid not null,
                         first_name varchar(255) not null,
                         last_name varchar(255) not null,
                         primary key (id)
);

create table if not exists locations (
                           building_name varchar(255),
                           building_number varchar(255),
                           city varchar(255),
                           country varchar(255),
                           description varchar(255),
                           id varchar(255) not null,
                           location_type_code varchar(255),
                           postal_code varchar(255),
                           street_name varchar(255),
                           primary key (id)
);

create table if not exists transports (
                          delivery_date_time timestamp(6) without time zone,
                          pickup_date_time timestamp(6) without time zone,
                          carrier_party_id uuid,
                          consignor_party_id uuid,
                          driver_id uuid,
                          id uuid NOT NULL,
                          delivery_location_id character varying(255),
                          pickup_location_id character varying(255),
                          transport_type text,
                          truck_id character varying(255),
                          display_number text,
                          container_id text,
                          note text,
                          container_operation text CHECK (container_operation IN ('EXCHANGE', 'EMPTY', 'PICKUP', 'DELIVERY', 'WAYBILL')),
                          sequence_number integer,
                          transport_hours NUMERIC(3,1),
                          driver_note text,
                          weight_ticket_id bigint,
                          created_at timestamp with time zone not null default now(),
                          created_by text,
                          last_modified_at timestamp with time zone not null default now(),
                          last_modified_by text,
                          primary key (id)
);

create table if not exists trucks (
                        last_modified_at timestamp(6) not null,
                        last_modified_by text,
                        created_at timestamp(6) not null,
                        created_by text,
                        brand text,
                        license_plate text not null,
                        description text,
                        carrier_party_id uuid,
                        primary key (license_plate)
);

create table if not exists waste_containers (
                           id text,
                           notes text,
                           location_id text,
                           created_at timestamp with time zone not null default now(),
                           created_by text,
                           last_modified_at timestamp with time zone not null default now(),
                           last_modified_by text,
                           primary key (id)
);

create table if not exists waybills (
                          delivery_date_time timestamp(6),
                          goods_item_id bigint unique,
                          pickup_date_time timestamp(6),
                          updated_at timestamp(6) not null,
                          carrier_party_id uuid,
                          consignee_party_id uuid,
                          consignor_party_id uuid,
                          pickup_party_id uuid,
                          uuid uuid not null,
                          delivery_location_id varchar(255) unique,
                          id varchar(255),
                          license_plate varchar(255),
                          note varchar(255),
                          pickup_location_id varchar(255) unique,
                          primary key (uuid)
);

create table if not exists profiles (
                           id uuid not null,
                           first_name text not null,
                           last_name text not null
);

create table if not exists user_roles (
                                     id bigint generated by default as identity not null,
                                     user_id uuid not null,
                                     role text not null
);

create table if not exists signatures (
                                       transport_id uuid not null,
                                       consignor_signature text,
                                       consignor_email text,
                                       consignor_signed_at timestamp with time zone,
                                       pickup_signature text,
                                       pickup_email text,
                                       pickup_signed_at timestamp with time zone,
                                       carrier_signature text,
                                       carrier_email text,
                                       carrier_signed_at timestamp with time zone,
                                       consignee_signature text,
                                       consignee_email text,
                                       consignee_signed_at timestamp with time zone
);

create table if not exists eural (
                                  code text not null,
                                  description text not null,
                                  created_at timestamp with time zone not null default now(),
                                  created_by text,
                                  last_modified_at timestamp with time zone not null default now(),
                                  last_modified_by text
);

create table if not exists processing_methods (
                                  code text not null,
                                  description text not null,
                                  created_at timestamp with time zone not null default now(),
                                  created_by text,
                                  last_modified_at timestamp with time zone not null default now(),
                                  last_modified_by text
);

create table if not exists waste_streams (
                                  number text not null,
                                  name text not null,
                                  eural_code text not null,
                                  processing_method_code text not null,
                                  waste_collection_type text not null,
                                  pickup_location_id text,
                                  consignor_party_id uuid not null,
                                  consignor_classification smallint,
                                  pickup_party_id uuid not null,
                                  dealer_party_id uuid,
                                  collector_party_id uuid,
                                  broker_party_id uuid,
                                  processor_party_id text,
                                  status text,
                                  created_at timestamp with time zone not null default now(),
                                  created_by text,
                                  last_modified_at timestamp with time zone not null default now(),
                                  last_modified_by text,
                                  primary key (number)
);

create table if not exists transport_goods (
                                          "id" bigint generated by default as identity not null,
                                          "transport_id" uuid not null,
                                          "net_net_weight" integer not null,
                                          "quantity" integer not null,
                                          "unit" text,
                                          "waste_stream_number" text not null
);
create table if not exists pickup_locations (
                                 id text not null,
                                 name text,
                                 created_at timestamp with time zone not null default now(),
                                 location_type text not null,
                                 street_name text,
                                 building_number text,
                                 building_number_addition text,
                                 country text,
                                 proximity_description text,
                                 city text,
                                 postal_code text,
                                 company_id uuid,
                                 primary key (id)
);

create sequence if not exists weight_tickets_id_seq start with 1 increment by 1;

create table if not exists weight_tickets (
                                 id bigint not null,
                                 consignor_party_id uuid not null,
                                 carrier_party_id uuid,
                                 direction text not null,
                                 pickup_location_id text,
                                 delivery_location_id text,
                                 truck_license_plate text,
                                 second_weighing_value numeric(10,2),
                                 second_weighing_unit text,
                                 tarra_weight_value numeric(10,2),
                                 tarra_weight_unit text,
                                 reclamation text,
                                 note text,
                                 status text not null,
                                 created_at timestamp with time zone not null default now(),
                                 created_by text,
                                 last_modified_at timestamp with time zone not null default now(),
                                 last_modified_by text,
                                 weighted_at timestamp with time zone ,
                                 cancellation_reason text,
                                 pdf_url text,
                                 primary key (id)
);

create table if not exists weight_ticket_lines (
                                 weight_ticket_id bigint not null,
                                 waste_stream_number text not null,
                                 weight_value numeric(10,2) not null,
                                 weight_unit text not null,
                                 constraint fk_weight_ticket_lines_weight_ticket foreign key (weight_ticket_id) references weight_tickets(id),
                                 constraint fk_weight_ticket_lines_waste_stream foreign key (waste_stream_number) references waste_streams(number)
);


create table lma_declaration_sessions (
                                                   "id" uuid not null,
                                                   "type" text not null,
                                                   "declaration_ids" text[] not null,
                                                   "status" text not null,
                                                   "errors" text[],
                                                   "created_at" timestamp with time zone not null,
                                                   "processed_at" timestamp with time zone
);

create table lma_declarations (
                                           "id" text not null,
                                           "amice_uuid" uuid,
                                           "waste_stream_number" text not null,
                                           "period" text not null,
                                           "transporters" text[] not null,
                                           "total_weight" bigint not null,
                                           "total_shipments" bigint not null,
                                           "created_at" timestamp with time zone not null default now(),
                                           "errors" text[],
                                           "status" text not null,
                                           constraint lma_declarations_amice_uuid_unique unique ("amice_uuid")
);

create table monthly_waste_declaration_jobs (
                                   id uuid not null,
                                   job_type text not null,
                                   year_month text not null,
                                   status text not null,
                                   created_at timestamp with time zone not null,
                                   fulfilled_at timestamp with time zone
);

create sequence if not exists receival_declaration_id_seq start with 1 increment by 1 maxvalue 999999999999;

create table if not exists vat_rates (
                                         vat_code text not null,
                                         percentage numeric not null,
                                         valid_from timestamp with time zone not null,
                                         valid_to timestamp with time zone,
                                         country_code text not null,
                                         description text not null,
                                         created_at timestamp with time zone not null default now(),
                                         created_by text,
                                         last_modified_at timestamp with time zone not null default now(),
                                         last_modified_by text,
                                         primary key (vat_code)
);

create table if not exists material_groups (
                                                id bigint generated always as identity,
                                                code text not null,
                                                name text not null,
                                                description text,
                                                created_at timestamp with time zone not null default now(),
                                                created_by text,
                                                last_modified_at timestamp with time zone not null default now(),
                                                last_modified_by text,
                                                primary key (id)
);

create table if not exists materials (
                                          id bigint generated always as identity,
                                          code text not null,
                                          name text not null,
                                          material_group_id bigint not null,
                                          unit_of_measure text not null,
                                          vat_code text not null,
                                          status text not null,
                                          created_at timestamp with time zone not null default now(),
                                          created_by text,
                                          last_modified_at timestamp with time zone not null default now(),
                                          last_modified_by text,
                                          primary key (id),
                                          foreign key (material_group_id) references material_groups(id),
                                          foreign key (vat_code) references vat_rates(vat_code)
);

create table if not exists material_prices (
                                               id bigint generated always as identity,
                                               material_id bigint not null,
                                               price numeric(19,4) not null,
                                               currency text not null,
                                               valid_from timestamp with time zone not null,
                                               valid_to timestamp with time zone,
                                               created_at timestamp with time zone not null default now(),
                                               created_by text,
                                               last_modified_at timestamp with time zone not null default now(),
                                               last_modified_by text,
                                               primary key (id),
                                               foreign key (material_id) references materials(id)
);

create table if not exists exact_tokens (
                                            id uuid not null,
                                            access_token text not null,
                                            refresh_token text not null,
                                            token_type text not null,
                                            expires_at timestamp with time zone not null,
                                            created_at timestamp with time zone not null default now(),
                                            updated_at timestamp with time zone not null default now(),
                                            primary key (id)
);

create table if not exists companies_sync (
                                               id uuid not null,
                                               company_id uuid not null,
                                               exact_guid uuid,
                                               last_timestamp bigint,
                                               sync_status text not null,
                                               synced_from_source_at timestamp with time zone,
                                               sync_error_message text,
                                               conflict_details jsonb,
                                               created_at timestamp with time zone not null,
                                               updated_at timestamp with time zone,
                                               deleted_in_source bool,
                                               deleted_in_exact_at timestamp with time zone,
                                               deleted_in_exact_by uuid,
                                               deleted_locally_at timestamp with time zone,
                                               primary key (id),
                                               foreign key (company_id) references companies(id)
);

create table if not exists companies_sync_cursor (
                                               id uuid not null,
                                               entity text not null,
                                               cursor_type text not null default 'sync',
                                               last_timestamp bigint not null,
                                               updated_at timestamp with time zone not null,
                                               primary key (id),
                                               unique (entity, cursor_type)
);
