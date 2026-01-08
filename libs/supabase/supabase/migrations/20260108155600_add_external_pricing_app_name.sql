alter table "public"."material_price_sync_log" alter column "material_id" drop not null;

alter table "public"."material_pricing_app_sync" add column "external_pricing_app_name" text not null;
