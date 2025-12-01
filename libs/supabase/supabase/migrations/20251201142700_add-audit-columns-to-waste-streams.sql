-- Add audit columns to waste_streams table
-- First drop existing last_activity_at column if it exists
alter table "public"."waste_streams" drop column if exists "last_activity_at";

-- Add audit columns to waste_streams table
alter table "public"."waste_streams" add column if not exists "created_at" timestamp with time zone not null default now();
alter table "public"."waste_streams" add column if not exists "created_by" text;
alter table "public"."waste_streams" add column if not exists "last_modified_at" timestamp with time zone not null default now();
alter table "public"."waste_streams" add column if not exists "last_modified_by" text;
