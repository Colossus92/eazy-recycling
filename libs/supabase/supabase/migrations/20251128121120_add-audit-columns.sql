alter table "public"."trucks" add column "created_at" timestamp with time zone not null default now();

alter table "public"."trucks" add column "created_by" text;

alter table "public"."trucks" add column "updated_by" text;


