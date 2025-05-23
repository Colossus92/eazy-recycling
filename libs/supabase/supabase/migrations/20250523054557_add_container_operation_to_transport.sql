create type "public"."container_operations" as enum ('EXCHANGE', 'EMPTY', 'PICKUP', 'DELIVERY', 'WAYBILL');

alter table "public"."transports" drop constraint "transports_transport_type_check";

alter table "public"."transports" add column "container_operation" text;

alter table "public"."transports" alter column "transport_type" set data type text using "transport_type"::text;

drop type "public"."transport_types";


