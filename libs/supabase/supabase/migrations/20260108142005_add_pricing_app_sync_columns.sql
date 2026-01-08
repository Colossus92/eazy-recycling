create sequence "public"."material_price_sync_log_id_seq";

create sequence "public"."material_pricing_app_sync_id_seq";


  create table "public"."material_price_sync_log" (
    "id" bigint not null default nextval('public.material_price_sync_log_id_seq'::regclass),
    "material_id" uuid not null,
    "external_product_id" integer,
    "action" text not null,
    "price_synced" numeric(19,4),
    "price_status_sent" integer,
    "status" text not null,
    "error_message" text,
    "synced_at" timestamp with time zone default now(),
    "synced_by" text
      );


alter table "public"."material_price_sync_log" enable row level security;


  create table "public"."material_pricing_app_sync" (
    "id" bigint not null default nextval('public.material_pricing_app_sync_id_seq'::regclass),
    "material_id" uuid not null,
    "publish_to_pricing_app" boolean not null default false,
    "external_pricing_app_id" integer,
    "external_pricing_app_synced_at" timestamp with time zone,
    "last_synced_price" numeric(19,4),
    "created_at" timestamp with time zone not null default now(),
    "last_modified_at" timestamp with time zone not null default now(),
    "created_by" text,
    "last_modified_by" text
      );


alter table "public"."material_pricing_app_sync" enable row level security;

alter table "public"."invoices" add column "credited_invoice_number" text;

alter sequence "public"."material_price_sync_log_id_seq" owned by "public"."material_price_sync_log"."id";

alter sequence "public"."material_pricing_app_sync_id_seq" owned by "public"."material_pricing_app_sync"."id";

CREATE UNIQUE INDEX companies_sync_company_id_unique ON public.companies_sync USING btree (company_id);

CREATE INDEX idx_material_price_sync_log_material_id ON public.material_price_sync_log USING btree (material_id);

CREATE INDEX idx_material_price_sync_log_synced_at ON public.material_price_sync_log USING btree (synced_at);

CREATE INDEX idx_material_pricing_app_sync_material_id ON public.material_pricing_app_sync USING btree (material_id);

CREATE INDEX idx_material_pricing_app_sync_publish ON public.material_pricing_app_sync USING btree (publish_to_pricing_app) WHERE (publish_to_pricing_app = true);

CREATE UNIQUE INDEX material_price_sync_log_pkey ON public.material_price_sync_log USING btree (id);

CREATE UNIQUE INDEX material_pricing_app_sync_material_id_key ON public.material_pricing_app_sync USING btree (material_id);

CREATE UNIQUE INDEX material_pricing_app_sync_pkey ON public.material_pricing_app_sync USING btree (id);

alter table "public"."material_price_sync_log" add constraint "material_price_sync_log_pkey" PRIMARY KEY using index "material_price_sync_log_pkey";

alter table "public"."material_pricing_app_sync" add constraint "material_pricing_app_sync_pkey" PRIMARY KEY using index "material_pricing_app_sync_pkey";

alter table "public"."companies_sync" add constraint "companies_sync_company_id_unique" UNIQUE using index "companies_sync_company_id_unique";

alter table "public"."material_price_sync_log" add constraint "material_price_sync_log_material_id_fkey" FOREIGN KEY (material_id) REFERENCES public.catalog_items(id) not valid;

alter table "public"."material_price_sync_log" validate constraint "material_price_sync_log_material_id_fkey";

alter table "public"."material_pricing_app_sync" add constraint "material_pricing_app_sync_material_id_fkey" FOREIGN KEY (material_id) REFERENCES public.catalog_items(id) ON DELETE CASCADE not valid;

alter table "public"."material_pricing_app_sync" validate constraint "material_pricing_app_sync_material_id_fkey";

alter table "public"."material_pricing_app_sync" add constraint "material_pricing_app_sync_material_id_key" UNIQUE using index "material_pricing_app_sync_material_id_key";

grant delete on table "public"."material_price_sync_log" to "anon";

grant insert on table "public"."material_price_sync_log" to "anon";

grant references on table "public"."material_price_sync_log" to "anon";

grant select on table "public"."material_price_sync_log" to "anon";

grant trigger on table "public"."material_price_sync_log" to "anon";

grant truncate on table "public"."material_price_sync_log" to "anon";

grant update on table "public"."material_price_sync_log" to "anon";

grant delete on table "public"."material_price_sync_log" to "authenticated";

grant insert on table "public"."material_price_sync_log" to "authenticated";

grant references on table "public"."material_price_sync_log" to "authenticated";

grant select on table "public"."material_price_sync_log" to "authenticated";

grant trigger on table "public"."material_price_sync_log" to "authenticated";

grant truncate on table "public"."material_price_sync_log" to "authenticated";

grant update on table "public"."material_price_sync_log" to "authenticated";

grant delete on table "public"."material_price_sync_log" to "postgres";

grant insert on table "public"."material_price_sync_log" to "postgres";

grant references on table "public"."material_price_sync_log" to "postgres";

grant select on table "public"."material_price_sync_log" to "postgres";

grant trigger on table "public"."material_price_sync_log" to "postgres";

grant truncate on table "public"."material_price_sync_log" to "postgres";

grant update on table "public"."material_price_sync_log" to "postgres";

grant delete on table "public"."material_price_sync_log" to "service_role";

grant insert on table "public"."material_price_sync_log" to "service_role";

grant references on table "public"."material_price_sync_log" to "service_role";

grant select on table "public"."material_price_sync_log" to "service_role";

grant trigger on table "public"."material_price_sync_log" to "service_role";

grant truncate on table "public"."material_price_sync_log" to "service_role";

grant update on table "public"."material_price_sync_log" to "service_role";

grant delete on table "public"."material_pricing_app_sync" to "anon";

grant insert on table "public"."material_pricing_app_sync" to "anon";

grant references on table "public"."material_pricing_app_sync" to "anon";

grant select on table "public"."material_pricing_app_sync" to "anon";

grant trigger on table "public"."material_pricing_app_sync" to "anon";

grant truncate on table "public"."material_pricing_app_sync" to "anon";

grant update on table "public"."material_pricing_app_sync" to "anon";

grant delete on table "public"."material_pricing_app_sync" to "authenticated";

grant insert on table "public"."material_pricing_app_sync" to "authenticated";

grant references on table "public"."material_pricing_app_sync" to "authenticated";

grant select on table "public"."material_pricing_app_sync" to "authenticated";

grant trigger on table "public"."material_pricing_app_sync" to "authenticated";

grant truncate on table "public"."material_pricing_app_sync" to "authenticated";

grant update on table "public"."material_pricing_app_sync" to "authenticated";

grant delete on table "public"."material_pricing_app_sync" to "postgres";

grant insert on table "public"."material_pricing_app_sync" to "postgres";

grant references on table "public"."material_pricing_app_sync" to "postgres";

grant select on table "public"."material_pricing_app_sync" to "postgres";

grant trigger on table "public"."material_pricing_app_sync" to "postgres";

grant truncate on table "public"."material_pricing_app_sync" to "postgres";

grant update on table "public"."material_pricing_app_sync" to "postgres";

grant delete on table "public"."material_pricing_app_sync" to "service_role";

grant insert on table "public"."material_pricing_app_sync" to "service_role";

grant references on table "public"."material_pricing_app_sync" to "service_role";

grant select on table "public"."material_pricing_app_sync" to "service_role";

grant trigger on table "public"."material_pricing_app_sync" to "service_role";

grant truncate on table "public"."material_pricing_app_sync" to "service_role";

grant update on table "public"."material_pricing_app_sync" to "service_role";

grant delete on table "public"."weight_ticket_product_lines" to "postgres";

grant insert on table "public"."weight_ticket_product_lines" to "postgres";

grant references on table "public"."weight_ticket_product_lines" to "postgres";

grant select on table "public"."weight_ticket_product_lines" to "postgres";

grant trigger on table "public"."weight_ticket_product_lines" to "postgres";

grant truncate on table "public"."weight_ticket_product_lines" to "postgres";

grant update on table "public"."weight_ticket_product_lines" to "postgres";


