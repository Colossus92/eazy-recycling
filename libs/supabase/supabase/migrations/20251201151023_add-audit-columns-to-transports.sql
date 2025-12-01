-- Add audit columns to transports table
-- First drop existing updated_at column if it exists
alter table "public"."transports" drop column if exists "updated_at";

-- Add audit columns to transports table
alter table "public"."transports" add column if not exists "created_at" timestamp with time zone not null default now();
alter table "public"."transports" add column if not exists "created_by" text;
alter table "public"."transports" add column if not exists "last_modified_at" timestamp with time zone not null default now();
alter table "public"."transports" add column if not exists "last_modified_by" text;
