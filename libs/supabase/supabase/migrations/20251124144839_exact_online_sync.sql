
  create table "public"."companies_sync" (
    "id" uuid not null default gen_random_uuid(),
    "company_id" uuid not null,
    "external_id" text,
    "last_timestamp" bigint,
    "synced_from_source_at" timestamp with time zone,
    "sync_error_message" text,
    "deleted_in_source" boolean not null,
    "created_at" timestamp with time zone not null,
    "updated_at" timestamp with time zone,
    "sync_status" text not null
      );


alter table "public"."companies_sync" enable row level security;


  create table "public"."companies_sync_cursor" (
    "id" uuid not null default gen_random_uuid(),
    "entity" text not null,
    "last_timestamp" bigint not null,
    "updated_at" timestamp with time zone not null default now()
      );


alter table "public"."companies_sync_cursor" enable row level security;

alter table "public"."companies" add column "email" text;

alter table "public"."companies" add column "is_customer" boolean not null default false;

alter table "public"."companies" add column "is_supplier" boolean not null default false;

alter table "public"."companies" add column "phone" text;

CREATE UNIQUE INDEX companies_sync_cursor_pkey ON public.companies_sync_cursor USING btree (id);

CREATE UNIQUE INDEX exact_sync_pkey ON public.companies_sync USING btree (id);

alter table "public"."companies_sync" add constraint "exact_sync_pkey" PRIMARY KEY using index "exact_sync_pkey";

alter table "public"."companies_sync_cursor" add constraint "companies_sync_cursor_pkey" PRIMARY KEY using index "companies_sync_cursor_pkey";

alter table "public"."companies_sync" add constraint "exact_sync_company_id_fkey" FOREIGN KEY (company_id) REFERENCES public.companies(id) not valid;

alter table "public"."companies_sync" validate constraint "exact_sync_company_id_fkey";

grant delete on table "public"."companies_sync" to "anon";

grant insert on table "public"."companies_sync" to "anon";

grant references on table "public"."companies_sync" to "anon";

grant select on table "public"."companies_sync" to "anon";

grant trigger on table "public"."companies_sync" to "anon";

grant truncate on table "public"."companies_sync" to "anon";

grant update on table "public"."companies_sync" to "anon";

grant delete on table "public"."companies_sync" to "authenticated";

grant insert on table "public"."companies_sync" to "authenticated";

grant references on table "public"."companies_sync" to "authenticated";

grant select on table "public"."companies_sync" to "authenticated";

grant trigger on table "public"."companies_sync" to "authenticated";

grant truncate on table "public"."companies_sync" to "authenticated";

grant update on table "public"."companies_sync" to "authenticated";

grant delete on table "public"."companies_sync" to "service_role";

grant insert on table "public"."companies_sync" to "service_role";

grant references on table "public"."companies_sync" to "service_role";

grant select on table "public"."companies_sync" to "service_role";

grant trigger on table "public"."companies_sync" to "service_role";

grant truncate on table "public"."companies_sync" to "service_role";

grant update on table "public"."companies_sync" to "service_role";

grant delete on table "public"."companies_sync_cursor" to "anon";

grant insert on table "public"."companies_sync_cursor" to "anon";

grant references on table "public"."companies_sync_cursor" to "anon";

grant select on table "public"."companies_sync_cursor" to "anon";

grant trigger on table "public"."companies_sync_cursor" to "anon";

grant truncate on table "public"."companies_sync_cursor" to "anon";

grant update on table "public"."companies_sync_cursor" to "anon";

grant delete on table "public"."companies_sync_cursor" to "authenticated";

grant insert on table "public"."companies_sync_cursor" to "authenticated";

grant references on table "public"."companies_sync_cursor" to "authenticated";

grant select on table "public"."companies_sync_cursor" to "authenticated";

grant trigger on table "public"."companies_sync_cursor" to "authenticated";

grant truncate on table "public"."companies_sync_cursor" to "authenticated";

grant update on table "public"."companies_sync_cursor" to "authenticated";

grant delete on table "public"."companies_sync_cursor" to "service_role";

grant insert on table "public"."companies_sync_cursor" to "service_role";

grant references on table "public"."companies_sync_cursor" to "service_role";

grant select on table "public"."companies_sync_cursor" to "service_role";

grant trigger on table "public"."companies_sync_cursor" to "service_role";

grant truncate on table "public"."companies_sync_cursor" to "service_role";

grant update on table "public"."companies_sync_cursor" to "service_role";


