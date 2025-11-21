
  create table "public"."exact_tokens" (
    "id" uuid not null,
    "access_token" text not null,
    "refresh_token" text not null,
    "token_type" text not null,
    "expires_at" timestamp with time zone not null,
    "created_at" timestamp with time zone not null default now(),
    "updated_at" timestamp with time zone not null default now()
      );


alter table "public"."material_groups" alter column "description" drop not null;

alter table "public"."materials" alter column "vat_code" set data type text using "vat_code"::text;

CREATE UNIQUE INDEX exact_tokens_pkey ON public.exact_tokens USING btree (id);

alter table "public"."exact_tokens" add constraint "exact_tokens_pkey" PRIMARY KEY using index "exact_tokens_pkey";

grant delete on table "public"."exact_tokens" to "anon";

grant insert on table "public"."exact_tokens" to "anon";

grant references on table "public"."exact_tokens" to "anon";

grant select on table "public"."exact_tokens" to "anon";

grant trigger on table "public"."exact_tokens" to "anon";

grant truncate on table "public"."exact_tokens" to "anon";

grant update on table "public"."exact_tokens" to "anon";

grant delete on table "public"."exact_tokens" to "authenticated";

grant insert on table "public"."exact_tokens" to "authenticated";

grant references on table "public"."exact_tokens" to "authenticated";

grant select on table "public"."exact_tokens" to "authenticated";

grant trigger on table "public"."exact_tokens" to "authenticated";

grant truncate on table "public"."exact_tokens" to "authenticated";

grant update on table "public"."exact_tokens" to "authenticated";

grant delete on table "public"."exact_tokens" to "service_role";

grant insert on table "public"."exact_tokens" to "service_role";

grant references on table "public"."exact_tokens" to "service_role";

grant select on table "public"."exact_tokens" to "service_role";

grant trigger on table "public"."exact_tokens" to "service_role";

grant truncate on table "public"."exact_tokens" to "service_role";

grant update on table "public"."exact_tokens" to "service_role";


