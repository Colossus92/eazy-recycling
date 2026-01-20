alter table "public"."transport_goods" alter column "net_net_weight" drop not null;

alter table "public"."transports" add column "delivery_date" date;

alter table "public"."transports" add column "delivery_timing_mode" text;

alter table "public"."transports" add column "delivery_window_end" time without time zone;

alter table "public"."transports" add column "delivery_window_start" time without time zone;

alter table "public"."transports" add column "pickup_date" date;

alter table "public"."transports" add column "pickup_timing_mode" text;

alter table "public"."transports" add column "pickup_window_end" time without time zone;

alter table "public"."transports" add column "pickup_window_start" time without time zone;


