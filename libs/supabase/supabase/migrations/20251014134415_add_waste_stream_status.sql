alter table "public"."waste_streams" add column "last_activity_at" timestamp with time zone not null default now();

alter table "public"."waste_streams" add column "status" text not null default 'DRAFT'::text;


