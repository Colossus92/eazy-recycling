create table "public"."company_branches" (
    "id" uuid not null default gen_random_uuid(),
    "company_id" uuid not null,
    "street_name" text not null,
    "postal_code" text not null,
    "city" text not null,
    "updated_at" timestamp with time zone default now(),
    "building_name" text,
    "building_number" text not null,
    "country" text
);


alter table "public"."company_branches" enable row level security;

CREATE UNIQUE INDEX branches_pkey ON public.company_branches USING btree (id);

alter table "public"."company_branches" add constraint "branches_pkey" PRIMARY KEY using index "branches_pkey";

alter table "public"."company_branches" add constraint "branches_company_id_fkey" FOREIGN KEY (company_id) REFERENCES companies(id) ON UPDATE CASCADE ON DELETE CASCADE not valid;

alter table "public"."company_branches" validate constraint "branches_company_id_fkey";

grant delete on table "public"."company_branches" to "anon";

grant insert on table "public"."company_branches" to "anon";

grant references on table "public"."company_branches" to "anon";

grant select on table "public"."company_branches" to "anon";

grant trigger on table "public"."company_branches" to "anon";

grant truncate on table "public"."company_branches" to "anon";

grant update on table "public"."company_branches" to "anon";

grant delete on table "public"."company_branches" to "authenticated";

grant insert on table "public"."company_branches" to "authenticated";

grant references on table "public"."company_branches" to "authenticated";

grant select on table "public"."company_branches" to "authenticated";

grant trigger on table "public"."company_branches" to "authenticated";

grant truncate on table "public"."company_branches" to "authenticated";

grant update on table "public"."company_branches" to "authenticated";

grant delete on table "public"."company_branches" to "service_role";

grant insert on table "public"."company_branches" to "service_role";

grant references on table "public"."company_branches" to "service_role";

grant select on table "public"."company_branches" to "service_role";

grant trigger on table "public"."company_branches" to "service_role";

grant truncate on table "public"."company_branches" to "service_role";

grant update on table "public"."company_branches" to "service_role";


