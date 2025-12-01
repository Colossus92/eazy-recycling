alter table "public"."trucks" drop column "updated_at";

alter table "public"."trucks" add column "created_at" timestamp with time zone not null default now();

alter table "public"."trucks" add column "created_by" text;

alter table "public"."trucks" add column "last_modified_at" timestamp with time zone not null default now();

alter table "public"."trucks" add column "last_modified_by" text;


