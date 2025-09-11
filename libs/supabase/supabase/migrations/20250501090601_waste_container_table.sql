create table "public"."waste_containers" (
    "uuid" uuid not null default gen_random_uuid(),
    "id" text not null,
    "type" text,
    "created_at" timestamp with time zone not null default now(),
    "updated_at" timestamp with time zone default now(),
    "location_company_id" uuid,
    "street_name" text,
    "postal_code" text,
    "city" text,
    "building_number" text,
    "building_name" text,
    "country" text,
    "notes" text
);


alter table "public"."waste_containers" enable row level security;

CREATE UNIQUE INDEX waste_containers_id_key ON public.waste_containers USING btree (id);

CREATE UNIQUE INDEX waste_containers_pkey ON public.waste_containers USING btree (uuid);

alter table "public"."waste_containers" add constraint "waste_containers_pkey" PRIMARY KEY using index "waste_containers_pkey";

alter table "public"."waste_containers" add constraint "waste_containers_id_key" UNIQUE using index "waste_containers_id_key";

alter table "public"."waste_containers" add constraint "waste_containers_location_company_id_fkey" FOREIGN KEY (location_company_id) REFERENCES companies(id) ON UPDATE CASCADE ON DELETE SET NULL not valid;

alter table "public"."waste_containers" validate constraint "waste_containers_location_company_id_fkey";

grant delete on table "public"."waste_containers" to "anon";

grant insert on table "public"."waste_containers" to "anon";

grant references on table "public"."waste_containers" to "anon";

grant select on table "public"."waste_containers" to "anon";

grant trigger on table "public"."waste_containers" to "anon";

grant truncate on table "public"."waste_containers" to "anon";

grant update on table "public"."waste_containers" to "anon";

grant delete on table "public"."waste_containers" to "authenticated";

grant insert on table "public"."waste_containers" to "authenticated";

grant references on table "public"."waste_containers" to "authenticated";

grant select on table "public"."waste_containers" to "authenticated";

grant trigger on table "public"."waste_containers" to "authenticated";

grant truncate on table "public"."waste_containers" to "authenticated";

grant update on table "public"."waste_containers" to "authenticated";

grant delete on table "public"."waste_containers" to "service_role";

grant insert on table "public"."waste_containers" to "service_role";

grant references on table "public"."waste_containers" to "service_role";

grant select on table "public"."waste_containers" to "service_role";

grant trigger on table "public"."waste_containers" to "service_role";

grant truncate on table "public"."waste_containers" to "service_role";

grant update on table "public"."waste_containers" to "service_role";


