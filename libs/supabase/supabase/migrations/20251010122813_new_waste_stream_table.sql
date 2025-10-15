revoke delete on table "public"."waste_stream" from "anon";

revoke insert on table "public"."waste_stream" from "anon";

revoke references on table "public"."waste_stream" from "anon";

revoke select on table "public"."waste_stream" from "anon";

revoke trigger on table "public"."waste_stream" from "anon";

revoke truncate on table "public"."waste_stream" from "anon";

revoke update on table "public"."waste_stream" from "anon";

revoke delete on table "public"."waste_stream" from "authenticated";

revoke insert on table "public"."waste_stream" from "authenticated";

revoke references on table "public"."waste_stream" from "authenticated";

revoke select on table "public"."waste_stream" from "authenticated";

revoke trigger on table "public"."waste_stream" from "authenticated";

revoke truncate on table "public"."waste_stream" from "authenticated";

revoke update on table "public"."waste_stream" from "authenticated";

revoke delete on table "public"."waste_stream" from "service_role";

revoke insert on table "public"."waste_stream" from "service_role";

revoke references on table "public"."waste_stream" from "service_role";

revoke select on table "public"."waste_stream" from "service_role";

revoke trigger on table "public"."waste_stream" from "service_role";

revoke truncate on table "public"."waste_stream" from "service_role";

revoke update on table "public"."waste_stream" from "service_role";

alter table "public"."goods_items" drop constraint "goods_items_waste_stream_number_fkey";

alter table "public"."waste_stream" drop constraint "waste_stream_pkey";

drop index if exists "public"."waste_stream_pkey";

drop table "public"."waste_stream";

create table "public"."pickup_locations" (
    "id" text not null,
    "created_at" timestamp with time zone not null default now(),
    "location_type" text not null,
    "building_number" text,
    "building_number_addition" text,
    "country" text,
    "proximity_description" text,
    "city" text,
    "postal_code" text
);


alter table "public"."pickup_locations" enable row level security;

create table "public"."waste_streams" (
    "number" text not null,
    "name" text not null,
    "eural_code" text not null,
    "processing_method_code" text not null,
    "waste_collection_type" text not null,
    "pickup_location_id" text,
    "consignor_party_id" uuid not null,
    "pickup_party_id" uuid not null,
    "dealer_party_id" uuid,
    "collector_party_id" uuid,
    "broker_party_id" uuid,
    "processor_party_id" text
);


alter table "public"."waste_streams" enable row level security;

alter table "public"."companies" add column "processor_id" text;

CREATE UNIQUE INDEX companies_processor_id_key ON public.companies USING btree (processor_id);

CREATE UNIQUE INDEX pickup_locations_pkey ON public.pickup_locations USING btree (id);

CREATE UNIQUE INDEX waste_streams_pkey ON public.waste_streams USING btree (number);

alter table "public"."pickup_locations" add constraint "pickup_locations_pkey" PRIMARY KEY using index "pickup_locations_pkey";

alter table "public"."waste_streams" add constraint "waste_streams_pkey" PRIMARY KEY using index "waste_streams_pkey";

alter table "public"."companies" add constraint "companies_processor_id_key" UNIQUE using index "companies_processor_id_key";

alter table "public"."waste_streams" add constraint "waste_streams_broker_party_id_fkey" FOREIGN KEY (broker_party_id) REFERENCES companies(id) not valid;

alter table "public"."waste_streams" validate constraint "waste_streams_broker_party_id_fkey";

alter table "public"."waste_streams" add constraint "waste_streams_collector_party_id_fkey" FOREIGN KEY (collector_party_id) REFERENCES companies(id) not valid;

alter table "public"."waste_streams" validate constraint "waste_streams_collector_party_id_fkey";

alter table "public"."waste_streams" add constraint "waste_streams_consignor_party_id_fkey" FOREIGN KEY (consignor_party_id) REFERENCES companies(id) not valid;

alter table "public"."waste_streams" validate constraint "waste_streams_consignor_party_id_fkey";

alter table "public"."waste_streams" add constraint "waste_streams_dealer_party_id_fkey" FOREIGN KEY (dealer_party_id) REFERENCES companies(id) not valid;

alter table "public"."waste_streams" validate constraint "waste_streams_dealer_party_id_fkey";

alter table "public"."waste_streams" add constraint "waste_streams_eural_code_fkey" FOREIGN KEY (eural_code) REFERENCES eural(code) not valid;

alter table "public"."waste_streams" validate constraint "waste_streams_eural_code_fkey";

alter table "public"."waste_streams" add constraint "waste_streams_pickup_location_id_fkey" FOREIGN KEY (pickup_location_id) REFERENCES pickup_locations(id) not valid;

alter table "public"."waste_streams" validate constraint "waste_streams_pickup_location_id_fkey";

alter table "public"."waste_streams" add constraint "waste_streams_pickup_party_id_fkey" FOREIGN KEY (pickup_party_id) REFERENCES companies(id) not valid;

alter table "public"."waste_streams" validate constraint "waste_streams_pickup_party_id_fkey";

alter table "public"."waste_streams" add constraint "waste_streams_processing_method_code_fkey" FOREIGN KEY (processing_method_code) REFERENCES processing_methods(code) not valid;

alter table "public"."waste_streams" validate constraint "waste_streams_processing_method_code_fkey";

alter table "public"."waste_streams" add constraint "waste_streams_processor_party_id_fkey" FOREIGN KEY (processor_party_id) REFERENCES companies(processor_id) not valid;

alter table "public"."waste_streams" validate constraint "waste_streams_processor_party_id_fkey";

grant delete on table "public"."pickup_locations" to "anon";

grant insert on table "public"."pickup_locations" to "anon";

grant references on table "public"."pickup_locations" to "anon";

grant select on table "public"."pickup_locations" to "anon";

grant trigger on table "public"."pickup_locations" to "anon";

grant truncate on table "public"."pickup_locations" to "anon";

grant update on table "public"."pickup_locations" to "anon";

grant delete on table "public"."pickup_locations" to "authenticated";

grant insert on table "public"."pickup_locations" to "authenticated";

grant references on table "public"."pickup_locations" to "authenticated";

grant select on table "public"."pickup_locations" to "authenticated";

grant trigger on table "public"."pickup_locations" to "authenticated";

grant truncate on table "public"."pickup_locations" to "authenticated";

grant update on table "public"."pickup_locations" to "authenticated";

grant delete on table "public"."pickup_locations" to "service_role";

grant insert on table "public"."pickup_locations" to "service_role";

grant references on table "public"."pickup_locations" to "service_role";

grant select on table "public"."pickup_locations" to "service_role";

grant trigger on table "public"."pickup_locations" to "service_role";

grant truncate on table "public"."pickup_locations" to "service_role";

grant update on table "public"."pickup_locations" to "service_role";

grant delete on table "public"."waste_streams" to "anon";

grant insert on table "public"."waste_streams" to "anon";

grant references on table "public"."waste_streams" to "anon";

grant select on table "public"."waste_streams" to "anon";

grant trigger on table "public"."waste_streams" to "anon";

grant truncate on table "public"."waste_streams" to "anon";

grant update on table "public"."waste_streams" to "anon";

grant delete on table "public"."waste_streams" to "authenticated";

grant insert on table "public"."waste_streams" to "authenticated";

grant references on table "public"."waste_streams" to "authenticated";

grant select on table "public"."waste_streams" to "authenticated";

grant trigger on table "public"."waste_streams" to "authenticated";

grant truncate on table "public"."waste_streams" to "authenticated";

grant update on table "public"."waste_streams" to "authenticated";

grant delete on table "public"."waste_streams" to "service_role";

grant insert on table "public"."waste_streams" to "service_role";

grant references on table "public"."waste_streams" to "service_role";

grant select on table "public"."waste_streams" to "service_role";

grant trigger on table "public"."waste_streams" to "service_role";

grant truncate on table "public"."waste_streams" to "service_role";

grant update on table "public"."waste_streams" to "service_role";
