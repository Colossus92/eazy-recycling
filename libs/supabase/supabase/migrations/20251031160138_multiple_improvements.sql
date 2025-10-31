create sequence "public"."weight_ticket_lines_id_seq";

revoke delete on table "public"."user_roles" from "anon";

revoke insert on table "public"."user_roles" from "anon";

revoke references on table "public"."user_roles" from "anon";

revoke select on table "public"."user_roles" from "anon";

revoke trigger on table "public"."user_roles" from "anon";

revoke truncate on table "public"."user_roles" from "anon";

revoke update on table "public"."user_roles" from "anon";

revoke delete on table "public"."user_roles" from "authenticated";

revoke insert on table "public"."user_roles" from "authenticated";

revoke references on table "public"."user_roles" from "authenticated";

revoke select on table "public"."user_roles" from "authenticated";

revoke trigger on table "public"."user_roles" from "authenticated";

revoke truncate on table "public"."user_roles" from "authenticated";

revoke update on table "public"."user_roles" from "authenticated";

alter table "public"."transports" drop constraint "transports_container_id_fkey";

alter table "public"."waste_containers" drop constraint "waste_containers_pkey";

drop index if exists "public"."waste_containers_pkey";

alter table "public"."pickup_locations" add column "name" text;

alter table "public"."transports" alter column "container_id" set data type text using "container_id"::text;

alter table "public"."waste_containers" drop column "uuid";

alter table "public"."waste_streams" add column "consignor_classification" smallint;

alter table "public"."weight_ticket_lines" add column "id" bigint not null default nextval('public.weight_ticket_lines_id_seq'::regclass);

alter table "public"."weight_tickets" add column "delivery_location_id" text;

alter table "public"."weight_tickets" add column "direction" text not null default ''::text;

alter table "public"."weight_tickets" add column "pickup_location_id" text;

alter table "public"."weight_tickets" add column "tarra_weight_unit" text;

alter table "public"."weight_tickets" add column "tarra_weight_value" numeric;

alter sequence "public"."weight_ticket_lines_id_seq" owned by "public"."weight_ticket_lines"."id";

CREATE UNIQUE INDEX weight_ticket_lines_pkey ON public.weight_ticket_lines USING btree (id);

CREATE UNIQUE INDEX waste_containers_pkey ON public.waste_containers USING btree (id);

alter table "public"."weight_ticket_lines" add constraint "weight_ticket_lines_pkey" PRIMARY KEY using index "weight_ticket_lines_pkey";

alter table "public"."waste_containers" add constraint "waste_containers_pkey" PRIMARY KEY using index "waste_containers_pkey";

alter table "public"."weight_tickets" add constraint "weight_tickets_delivery_location_id_fkey" FOREIGN KEY (delivery_location_id) REFERENCES public.pickup_locations(id) not valid;

alter table "public"."weight_tickets" validate constraint "weight_tickets_delivery_location_id_fkey";

alter table "public"."weight_tickets" add constraint "weight_tickets_pickup_location_id_fkey" FOREIGN KEY (pickup_location_id) REFERENCES public.pickup_locations(id) not valid;

alter table "public"."weight_tickets" validate constraint "weight_tickets_pickup_location_id_fkey";

alter table "public"."transports" add constraint "transports_container_id_fkey" FOREIGN KEY (container_id) REFERENCES public.waste_containers(id) not valid;

alter table "public"."transports" validate constraint "transports_container_id_fkey";

CREATE TRIGGER tr_check_filters BEFORE INSERT OR UPDATE ON realtime.subscription FOR EACH ROW EXECUTE FUNCTION realtime.subscription_check_filters();


