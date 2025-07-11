create table "public"."processing_methods" (
    "code" text not null,
    "description" text not null
);


alter table "public"."processing_methods" enable row level security;

alter table "public"."goods_items" add column "processing_method_code" text;

alter table "public"."goods_items" alter column "eural_code" set data type text using "eural_code"::text;

CREATE UNIQUE INDEX processing_methods_pkey ON public.processing_methods USING btree (code);

alter table "public"."processing_methods" add constraint "processing_methods_pkey" PRIMARY KEY using index "processing_methods_pkey";

alter table "public"."goods_items" add constraint "goods_items_processing_method_fkey" FOREIGN KEY (processing_method_code) REFERENCES processing_methods(code) not valid;

alter table "public"."goods_items" validate constraint "goods_items_processing_method_fkey";

grant delete on table "public"."processing_methods" to "anon";

grant insert on table "public"."processing_methods" to "anon";

grant references on table "public"."processing_methods" to "anon";

grant select on table "public"."processing_methods" to "anon";

grant trigger on table "public"."processing_methods" to "anon";

grant truncate on table "public"."processing_methods" to "anon";

grant update on table "public"."processing_methods" to "anon";

grant delete on table "public"."processing_methods" to "authenticated";

grant insert on table "public"."processing_methods" to "authenticated";

grant references on table "public"."processing_methods" to "authenticated";

grant select on table "public"."processing_methods" to "authenticated";

grant trigger on table "public"."processing_methods" to "authenticated";

grant truncate on table "public"."processing_methods" to "authenticated";

grant update on table "public"."processing_methods" to "authenticated";

grant delete on table "public"."processing_methods" to "service_role";

grant insert on table "public"."processing_methods" to "service_role";

grant references on table "public"."processing_methods" to "service_role";

grant select on table "public"."processing_methods" to "service_role";

grant trigger on table "public"."processing_methods" to "service_role";

grant truncate on table "public"."processing_methods" to "service_role";

grant update on table "public"."processing_methods" to "service_role";


