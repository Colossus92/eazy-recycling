-- Add audit columns to weight tickets table
-- First drop existing updated_at column if it exists
alter table "public"."weight_tickets" drop column if exists "updated_at";
alter table "public"."weight_tickets" drop column if exists "created_at";

-- Add audit columns to processing_methods table
alter table "public"."weight_tickets" add column if not exists "created_at" timestamp with time zone not null default now();
alter table "public"."weight_tickets" add column if not exists "created_by" text;
alter table "public"."weight_tickets" add column if not exists "last_modified_at" timestamp with time zone not null default now();
alter table "public"."weight_tickets" add column if not exists "last_modified_by" text;
