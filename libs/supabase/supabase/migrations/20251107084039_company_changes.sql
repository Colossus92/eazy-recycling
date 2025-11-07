alter table "public"."companies" add column "deleted_at" timestamp with time zone;

alter table "public"."companies" alter column "building_number" set data type text using "building_number"::text;

alter table "public"."companies" alter column "building_number_addition" set data type text using "building_number_addition"::text;

alter table "public"."companies" alter column "chamber_of_commerce_id" set data type text using "chamber_of_commerce_id"::text;

alter table "public"."companies" alter column "city" set data type text using "city"::text;

alter table "public"."companies" alter column "country" set data type text using "country"::text;

alter table "public"."companies" alter column "name" set data type text using "name"::text;

alter table "public"."companies" alter column "postal_code" set data type text using "postal_code"::text;

alter table "public"."companies" alter column "street_name" set data type text using "street_name"::text;

alter table "public"."companies" alter column "vihb_id" set data type text using "vihb_id"::text;

set check_function_bodies = off;

