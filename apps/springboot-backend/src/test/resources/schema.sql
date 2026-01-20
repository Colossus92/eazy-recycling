create schema if not exists "jobrunr";

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
                           is_tenant_company boolean not null default false,
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
                          id uuid NOT NULL,
                          carrier_party_id uuid,
                          consignor_party_id uuid,
                          driver_id uuid,
                          delivery_location_id character varying(255),
                          pickup_location_id character varying(255),
                          -- Pickup timing constraint fields
                          pickup_date date not null,
                          pickup_timing_mode text not null,
                          pickup_window_start time,
                          pickup_window_end time,
                          -- Delivery timing constraint fields
                          delivery_date date,
                          delivery_timing_mode text,
                          delivery_window_start time,
                          delivery_window_end time,
                          transport_type text,
                          truck_id character varying(255),
                          display_number text,
                          container_id text,
                          note text,
                          container_operation text CHECK (container_operation IN ('EXCHANGE', 'EMPTY', 'PICKUP', 'DELIVERY', 'WAYBILL')),
                          sequence_number integer,
                          transport_hours NUMERIC(3,1),
                          driver_note text,
                          weight_ticket_id uuid,
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
                                     id uuid not null,
                                     user_id uuid not null,
                                     role text not null,
                                     primary key (id)
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
                                  catalog_item_id uuid,
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
                                 id uuid not null,
                                 number bigint not null unique,
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
                                 cancellation_reason text,
                                 linked_invoice_id uuid,
                                 pdf_url text,
                                 weighted_at timestamp with time zone,
                                 created_at timestamp with time zone not null default now(),
                                 created_by text,
                                 last_modified_at timestamp with time zone not null default now(),
                                 last_modified_by text,
                                 primary key (id)
);

create table if not exists weight_ticket_lines (
                                 id uuid not null,
                                 weight_ticket_id uuid not null,
                                 waste_stream_number text,
                                 catalog_item_id uuid not null,
                                 weight_value numeric(10,2) not null,
                                 weight_unit text not null,
                                 declared_weight numeric(10,2) default null,
                                 last_declared_at timestamp with time zone default null,
                                 primary key (id),
                                 constraint fk_weight_ticket_lines_weight_ticket foreign key (weight_ticket_id) references weight_tickets(id)
);

create index if not exists idx_weight_ticket_lines_declaration_state
  on weight_ticket_lines(declared_weight, last_declared_at)
  where declared_weight is not null;

create table if not exists weight_ticket_product_lines (
                                 id uuid not null,
                                 weight_ticket_id uuid not null,
                                 catalog_item_id uuid not null,
                                 quantity numeric(15,4) not null,
                                 unit text not null,
                                 primary key (id),
                                 constraint fk_weight_ticket_product_lines_weight_ticket foreign key (weight_ticket_id) references weight_tickets(id)
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
                                           "id" text not null unique,
                                           "amice_uuid" uuid,
                                           "waste_stream_number" text not null,
                                           "period" text not null,
                                           "transporters" text[] not null,
                                           "total_weight" bigint not null,
                                           "total_shipments" bigint not null,
                                           "type" text not null,
                                           "created_at" timestamp with time zone not null default now(),
                                           "errors" text[],
                                           "status" text not null,
                                           "weight_ticket_ids" uuid[],
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

create table if not exists lma_import_errors (
                                               id uuid not null,
                                               import_batch_id uuid not null,
                                               row_number int not null,
                                               waste_stream_number text,
                                               error_code text not null,
                                               error_message text not null,
                                               raw_data jsonb,
                                               created_at timestamp with time zone not null default now(),
                                               resolved_at timestamp with time zone,
                                               resolved_by text,
                                               primary key (id)
);

create table if not exists catalog_item_categories (
                                          id uuid not null,
                                          type text not null,
                                          code text not null unique,
                                          name text not null,
                                          description text,
                                          created_at timestamp with time zone not null default now(),
                                          created_by text,
                                          last_modified_at timestamp with time zone not null default now(),
                                          last_modified_by text,
                                          primary key (id)
);

create table if not exists catalog_items (
                                          id uuid not null,
                                          type text not null,
                                          code text not null unique,
                                          name text not null,
                                          category_id uuid,
                                          consignor_party_id uuid,
                                          unit_of_measure text not null,
                                          vat_code text not null,
                                          sales_account_number text,
                                          purchase_account_number text,
                                          default_price numeric(15,4),
                                          status text not null,
                                          created_at timestamp with time zone not null default now(),
                                          created_by text,
                                          last_modified_at timestamp with time zone not null default now(),
                                          last_modified_by text,
                                          primary key (id),
                                          foreign key (category_id) references catalog_item_categories(id),
                                          foreign key (vat_code) references vat_rates(vat_code)
);

-- Material pricing app sync tables
create table if not exists material_pricing_app_sync (
                                               id bigint generated always as identity,
                                               material_id uuid not null unique,
                                               publish_to_pricing_app boolean not null default false,
                                               external_pricing_app_id integer,
                                               external_pricing_app_synced_at timestamp with time zone,
                                               external_pricing_app_name text,
                                               last_synced_price numeric(19,4),
                                               created_at timestamp with time zone not null default now(),
                                               created_by text,
                                               last_modified_at timestamp with time zone not null default now(),
                                               last_modified_by text,
                                               primary key (id),
                                               foreign key (material_id) references catalog_items(id) on delete cascade
);

create table if not exists material_price_sync_log (
                                               id bigint generated always as identity,
                                               material_id uuid,
                                               external_product_id integer,
                                               action text not null,
                                               price_synced numeric(19,4),
                                               price_status_sent integer,
                                               status text not null,
                                               error_message text,
                                               synced_at timestamp with time zone not null default now(),
                                               synced_by text,
                                               primary key (id),
                                               foreign key (material_id) references catalog_items(id)
);

-- Invoice tables
create table if not exists invoices (
                                          id uuid not null,
                                          invoice_number text unique,
                                          invoice_type text not null check (invoice_type in ('PURCHASE', 'SALE')),
                                          document_type text not null check (document_type in ('INVOICE', 'CREDIT_NOTE')),
                                          status text not null check (status in ('DRAFT', 'FINAL')),
                                          invoice_date date not null,
                                          customer_company_id uuid not null,
                                          customer_number text,
                                          customer_name text not null,
                                          customer_street_name text not null,
                                          customer_building_number text,
                                          customer_building_number_addition text,
                                          customer_postal_code text not null,
                                          customer_city text not null,
                                          customer_country text,
                                          customer_vat_number text,
                                          original_invoice_id uuid,
                                          credited_invoice_number text,
                                          source_weight_ticket_id uuid,
                                          created_at timestamp with time zone not null default now(),
                                          created_by text,
                                          last_modified_at timestamp with time zone not null default now(),
                                          last_modified_by text,
                                          finalized_at timestamp with time zone,
                                          finalized_by text,
                                          pdf_url text,
                                          primary key (id),
                                          foreign key (customer_company_id) references companies(id),
                                          foreign key (original_invoice_id) references invoices(id)
);

create table if not exists invoice_lines (
                                          id uuid not null,
                                          invoice_id uuid not null,
                                          line_number int not null,
                                          line_date date not null,
                                          description text not null,
                                          order_reference text,
                                          vat_code text not null,
                                          vat_percentage numeric(5,2) not null,
                                          gl_account_code text,
                                          quantity numeric(15,4) not null,
                                          unit_price numeric(15,4) not null,
                                          total_excl_vat numeric(15,2) not null,
                                          unit_of_measure text not null,
                                          catalog_item_id uuid,
                                          catalog_item_code text not null,
                                          catalog_item_name text not null,
                                          catalog_item_type text not null check (catalog_item_type in ('MATERIAL', 'PRODUCT', 'WASTE_STREAM')),
                                          primary key (id),
                                         foreign key (invoice_id) references invoices(id) on delete cascade,
                                         foreign key (catalog_item_id) references catalog_items(id),
                                         unique (invoice_id, line_number)
);

CREATE SEQUENCE IF NOT EXISTS edge_function_outbox_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS edge_function_outbox (
    id bigint not null default nextval('edge_function_outbox_id_seq'),
    function_name text not null,
    http_method text not null,
    payload jsonb,
    status text not null default 'PENDING',
    attempts int not null default 0,
    last_attempt_at timestamp with time zone,
    error_message text,
    processed_at timestamp with time zone,
    created_at timestamp with time zone not null default now(),
    aggregate_type text,
    aggregate_id text,
    primary key (id),
    constraint chk_function_name check (function_name in ('INVOICE_PDF_GENERATOR')),
    constraint chk_http_method check (http_method in ('GET', 'POST', 'PUT', 'DELETE')),
    constraint chk_status check (status in ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_edge_function_outbox_status ON edge_function_outbox(status);
CREATE INDEX IF NOT EXISTS idx_edge_function_outbox_created_at ON edge_function_outbox(created_at);

CREATE TABLE IF NOT EXISTS invoice_number_sequences (
    year int not null primary key,
    last_sequence bigint not null default 0
);
