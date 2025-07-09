create table "public"."eural" (
    "code" text not null,
    "description" text not null
);


alter table "public"."eural" enable row level security;

CREATE UNIQUE INDEX eural_pkey ON public.eural USING btree (code);

alter table "public"."eural" add constraint "eural_pkey" PRIMARY KEY using index "eural_pkey";

grant delete on table "public"."eural" to "anon";

grant insert on table "public"."eural" to "anon";

grant references on table "public"."eural" to "anon";

grant select on table "public"."eural" to "anon";

grant trigger on table "public"."eural" to "anon";

grant truncate on table "public"."eural" to "anon";

grant update on table "public"."eural" to "anon";

grant delete on table "public"."eural" to "authenticated";

grant insert on table "public"."eural" to "authenticated";

grant references on table "public"."eural" to "authenticated";

grant select on table "public"."eural" to "authenticated";

grant trigger on table "public"."eural" to "authenticated";

grant truncate on table "public"."eural" to "authenticated";

grant update on table "public"."eural" to "authenticated";

grant delete on table "public"."eural" to "service_role";

grant insert on table "public"."eural" to "service_role";

grant references on table "public"."eural" to "service_role";

grant select on table "public"."eural" to "service_role";

grant trigger on table "public"."eural" to "service_role";

grant truncate on table "public"."eural" to "service_role";

grant update on table "public"."eural" to "service_role";


