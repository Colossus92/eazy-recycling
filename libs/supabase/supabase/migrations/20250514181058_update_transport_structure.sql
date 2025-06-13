drop policy "Enable delete for authenticated users only" on "public"."drivers";

drop policy "Enable insert for authenticated users only" on "public"."drivers";

drop policy "Enable read access for all users" on "public"."drivers";

drop policy "Enable update fora authenticated users only" on "public"."drivers";

revoke delete on table "public"."drivers" from "anon";

revoke insert on table "public"."drivers" from "anon";

revoke references on table "public"."drivers" from "anon";

revoke select on table "public"."drivers" from "anon";

revoke trigger on table "public"."drivers" from "anon";

revoke truncate on table "public"."drivers" from "anon";

revoke update on table "public"."drivers" from "anon";

revoke delete on table "public"."drivers" from "authenticated";

revoke insert on table "public"."drivers" from "authenticated";

revoke references on table "public"."drivers" from "authenticated";

revoke select on table "public"."drivers" from "authenticated";

revoke trigger on table "public"."drivers" from "authenticated";

revoke truncate on table "public"."drivers" from "authenticated";

revoke update on table "public"."drivers" from "authenticated";

revoke delete on table "public"."drivers" from "service_role";

revoke insert on table "public"."drivers" from "service_role";

revoke references on table "public"."drivers" from "service_role";

revoke select on table "public"."drivers" from "service_role";

revoke trigger on table "public"."drivers" from "service_role";

revoke truncate on table "public"."drivers" from "service_role";

revoke update on table "public"."drivers" from "service_role";

alter table "public"."transports" drop constraint "fk163l7w977al0i5bepjq1207ne";

alter table "public"."transports" drop constraint "fki82vj9n4pe2k5fbgdmj8mc8tr";

alter table "public"."transports" drop constraint "fkr7jo64ck7hsa4d9i2mucxlrjv";

alter table "public"."trips" drop constraint "trips_driver_id_fkey";

drop view if exists "public"."planning";

alter table "public"."drivers" drop constraint "driver_pkey";

drop index if exists "public"."driver_pkey";

drop table "public"."drivers";

alter table "public"."transports" drop column "container_type";

alter table "public"."transports" add column "container_id" uuid;

alter table "public"."transports" add constraint "transports_container_id_fkey" FOREIGN KEY (container_id) REFERENCES waste_containers(uuid) not valid;

alter table "public"."transports" validate constraint "transports_container_id_fkey";

alter table "public"."transports" add constraint "transports_delivery_location_id_fkey" FOREIGN KEY (delivery_location_id) REFERENCES locations(id) not valid;

alter table "public"."transports" validate constraint "transports_delivery_location_id_fkey";

alter table "public"."transports" add constraint "transports_driver_id_fkey" FOREIGN KEY (driver_id) REFERENCES profiles(id) not valid;

alter table "public"."transports" validate constraint "transports_driver_id_fkey";

alter table "public"."transports" add constraint "transports_pickup_location_id_fkey" FOREIGN KEY (pickup_location_id) REFERENCES locations(id) not valid;

alter table "public"."transports" validate constraint "transports_pickup_location_id_fkey";

alter table "public"."trips" add constraint "trips_driver_id_fkey1" FOREIGN KEY (driver_id) REFERENCES profiles(id) not valid;

alter table "public"."trips" validate constraint "trips_driver_id_fkey1";


