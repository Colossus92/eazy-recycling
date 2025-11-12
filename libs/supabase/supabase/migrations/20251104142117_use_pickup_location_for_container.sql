alter table "public"."waste_containers" add column "location_id" text;

alter table "public"."waste_containers" add constraint "waste_containers_location_id_fkey" FOREIGN KEY (location_id) REFERENCES public.pickup_locations(id) not valid;

alter table "public"."waste_containers" validate constraint "waste_containers_location_id_fkey";
