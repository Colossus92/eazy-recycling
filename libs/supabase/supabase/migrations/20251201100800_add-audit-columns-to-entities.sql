-- Add audit columns to companies table
-- First drop existing updated_at column if it exists
alter table "public"."companies" drop column if exists "updated_at";

alter table "public"."companies" add column if not exists "created_at" timestamp with time zone not null default now();
alter table "public"."companies" add column if not exists "created_by" text;
alter table "public"."companies" add column if not exists "last_modified_at" timestamp with time zone not null default now();
alter table "public"."companies" add column if not exists "last_modified_by" text;

-- Add audit columns to materials table
alter table "public"."materials" drop column if exists "created_at";
alter table "public"."materials" drop column if exists "updated_at";

alter table "public"."materials" add column "created_at" timestamp with time zone not null default now();
alter table "public"."materials" add column "created_by" text;
alter table "public"."materials" add column "last_modified_at" timestamp with time zone not null default now();
alter table "public"."materials" add column "last_modified_by" text;

-- Add audit columns to material_groups table
alter table "public"."material_groups" drop column if exists "created_at";
alter table "public"."material_groups" drop column if exists "updated_at";

alter table "public"."material_groups" add column "created_at" timestamp with time zone not null default now();
alter table "public"."material_groups" add column "created_by" text;
alter table "public"."material_groups" add column "last_modified_at" timestamp with time zone not null default now();
alter table "public"."material_groups" add column "last_modified_by" text;

-- Add audit columns to material_prices table
alter table "public"."material_prices" add column if not exists "created_at" timestamp with time zone not null default now();
alter table "public"."material_prices" add column if not exists "created_by" text;
alter table "public"."material_prices" add column if not exists "last_modified_at" timestamp with time zone not null default now();
alter table "public"."material_prices" add column if not exists "last_modified_by" text;

-- Add audit columns to vat_rates table
alter table "public"."vat_rates" add column if not exists "created_at" timestamp with time zone not null default now();
alter table "public"."vat_rates" add column if not exists "created_by" text;
alter table "public"."vat_rates" add column if not exists "last_modified_at" timestamp with time zone not null default now();
alter table "public"."vat_rates" add column if not exists "last_modified_by" text;

-- Add audit columns to waste_containers table
alter table "public"."waste_containers" add column if not exists "created_at" timestamp with time zone not null default now();
alter table "public"."waste_containers" add column if not exists "created_by" text;
alter table "public"."waste_containers" add column if not exists "last_modified_at" timestamp with time zone not null default now();
alter table "public"."waste_containers" add column if not exists "last_modified_by" text;

-- Add audit columns to eural table
alter table "public"."eural" add column if not exists "created_at" timestamp with time zone not null default now();
alter table "public"."eural" add column if not exists "created_by" text;
alter table "public"."eural" add column if not exists "last_modified_at" timestamp with time zone not null default now();
alter table "public"."eural" add column if not exists "last_modified_by" text;

-- Add audit columns to processing_methods table
alter table "public"."processing_methods" add column if not exists "created_at" timestamp with time zone not null default now();
alter table "public"."processing_methods" add column if not exists "created_by" text;
alter table "public"."processing_methods" add column if not exists "last_modified_at" timestamp with time zone not null default now();
alter table "public"."processing_methods" add column if not exists "last_modified_by" text;
