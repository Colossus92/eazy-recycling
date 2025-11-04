revoke delete on table "public"."company_branches" from "anon";

revoke insert on table "public"."company_branches" from "anon";

revoke references on table "public"."company_branches" from "anon";

revoke select on table "public"."company_branches" from "anon";

revoke trigger on table "public"."company_branches" from "anon";

revoke truncate on table "public"."company_branches" from "anon";

revoke update on table "public"."company_branches" from "anon";

revoke delete on table "public"."company_branches" from "authenticated";

revoke insert on table "public"."company_branches" from "authenticated";

revoke references on table "public"."company_branches" from "authenticated";

revoke select on table "public"."company_branches" from "authenticated";

revoke trigger on table "public"."company_branches" from "authenticated";

revoke truncate on table "public"."company_branches" from "authenticated";

revoke update on table "public"."company_branches" from "authenticated";

revoke delete on table "public"."company_branches" from "service_role";

revoke insert on table "public"."company_branches" from "service_role";

revoke references on table "public"."company_branches" from "service_role";

revoke select on table "public"."company_branches" from "service_role";

revoke trigger on table "public"."company_branches" from "service_role";

revoke truncate on table "public"."company_branches" from "service_role";

revoke update on table "public"."company_branches" from "service_role";

alter table "public"."company_branches" drop constraint "branches_company_id_fkey";

drop function if exists "public"."crypt"(password text, salt text);

drop function if exists "public"."gen_random_uuid"();

drop function if exists "public"."gen_salt"(type text);

alter table "public"."company_branches" drop constraint "branches_pkey";

drop index if exists "public"."branches_pkey";

drop table "public"."company_branches";


  create table "public"."company_project_locations" (
    "building_number" text not null,
    "building_number_addition" text,
    "city" text not null,
    "postal_code" text not null,
    "street_name" text not null,
    "company_id" uuid not null,
    "created_at" timestamp with time zone not null default now(),
    "updated_at" timestamp with time zone,
    "id" uuid not null,
    "country" text not null
      );


alter table "public"."company_project_locations" enable row level security;

CREATE UNIQUE INDEX company_project_locations_pkey ON public.company_project_locations USING btree (id);

alter table "public"."company_project_locations" add constraint "company_project_locations_pkey" PRIMARY KEY using index "company_project_locations_pkey";

alter table "public"."company_project_locations" add constraint "company_project_locations_company_id_fkey" FOREIGN KEY (company_id) REFERENCES public.companies(id) not valid;

alter table "public"."company_project_locations" validate constraint "company_project_locations_company_id_fkey";

grant delete on table "public"."company_project_locations" to "anon";

grant insert on table "public"."company_project_locations" to "anon";

grant references on table "public"."company_project_locations" to "anon";

grant select on table "public"."company_project_locations" to "anon";

grant trigger on table "public"."company_project_locations" to "anon";

grant truncate on table "public"."company_project_locations" to "anon";

grant update on table "public"."company_project_locations" to "anon";

grant delete on table "public"."company_project_locations" to "authenticated";

grant insert on table "public"."company_project_locations" to "authenticated";

grant references on table "public"."company_project_locations" to "authenticated";

grant select on table "public"."company_project_locations" to "authenticated";

grant trigger on table "public"."company_project_locations" to "authenticated";

grant truncate on table "public"."company_project_locations" to "authenticated";

grant update on table "public"."company_project_locations" to "authenticated";

grant delete on table "public"."company_project_locations" to "service_role";

grant insert on table "public"."company_project_locations" to "service_role";

grant references on table "public"."company_project_locations" to "service_role";

grant select on table "public"."company_project_locations" to "service_role";

grant trigger on table "public"."company_project_locations" to "service_role";

grant truncate on table "public"."company_project_locations" to "service_role";

grant update on table "public"."company_project_locations" to "service_role";
