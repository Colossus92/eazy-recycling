alter table "public"."materials" add column "purchase_account_number" text;

alter table "public"."materials" add column "sales_account_number" text;

alter table "public"."materials" alter column "material_group_id" drop not null;


