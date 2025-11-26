alter table "public"."companies" add column "deleted_by" uuid;

alter table "public"."companies_sync" add column "conflict_details" jsonb;

alter table "public"."companies_sync" add column "deleted_in_exact_at" timestamp with time zone;

alter table "public"."companies_sync" add column "deleted_in_exact_by" uuid;

alter table "public"."companies_sync" add column "deleted_locally_at" timestamp with time zone;

alter table "public"."companies_sync" add column "exact_guid" uuid;

alter table "public"."companies_sync" add column "requires_manual_review" boolean not null;

alter table "public"."companies_sync_cursor" add column "cursor_type" text not null default 'sync'::text;

alter table "public"."transport_goods" alter column "net_net_weight" set data type numeric using "net_net_weight"::numeric;

CREATE UNIQUE INDEX companies_sync_cursor_entity_type_unique ON public.companies_sync_cursor USING btree (entity, cursor_type);

alter table "public"."companies_sync_cursor" add constraint "companies_sync_cursor_entity_type_unique" UNIQUE using index "companies_sync_cursor_entity_type_unique";


