create table "public"."waste_stream" (
    "number" text not null,
    "name" text not null
);


alter table "public"."waste_stream" enable row level security;

alter table "public"."goods_items" alter column "name" set data type text using "name"::text;

alter table "public"."goods_items" alter column "unit" set data type text using "unit"::text;

alter table "public"."goods_items" alter column "waste_stream_number" set data type text using "waste_stream_number"::text;

CREATE UNIQUE INDEX waste_stream_pkey ON public.waste_stream USING btree (number);

alter table "public"."waste_stream" add constraint "waste_stream_pkey" PRIMARY KEY using index "waste_stream_pkey";

alter table "public"."goods_items" add constraint "goods_items_waste_stream_number_fkey" FOREIGN KEY (waste_stream_number) REFERENCES waste_stream(number) not valid;

alter table "public"."goods_items" validate constraint "goods_items_waste_stream_number_fkey";

grant delete on table "public"."waste_stream" to "anon";

grant insert on table "public"."waste_stream" to "anon";

grant references on table "public"."waste_stream" to "anon";

grant select on table "public"."waste_stream" to "anon";

grant trigger on table "public"."waste_stream" to "anon";

grant truncate on table "public"."waste_stream" to "anon";

grant update on table "public"."waste_stream" to "anon";

grant delete on table "public"."waste_stream" to "authenticated";

grant insert on table "public"."waste_stream" to "authenticated";

grant references on table "public"."waste_stream" to "authenticated";

grant select on table "public"."waste_stream" to "authenticated";

grant trigger on table "public"."waste_stream" to "authenticated";

grant truncate on table "public"."waste_stream" to "authenticated";

grant update on table "public"."waste_stream" to "authenticated";

grant delete on table "public"."waste_stream" to "service_role";

grant insert on table "public"."waste_stream" to "service_role";

grant references on table "public"."waste_stream" to "service_role";

grant select on table "public"."waste_stream" to "service_role";

grant trigger on table "public"."waste_stream" to "service_role";

grant truncate on table "public"."waste_stream" to "service_role";

grant update on table "public"."waste_stream" to "service_role";


