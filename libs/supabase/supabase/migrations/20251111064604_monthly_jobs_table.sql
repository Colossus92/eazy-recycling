
  create table "public"."lma_declaration_sessions" (
    "id" uuid not null,
    "type" text not null,
    "declaration_ids" text[] not null,
    "status" text not null,
    "errors" text[],
    "created_at" timestamp with time zone not null,
    "processed_at" timestamp with time zone
      );


alter table "public"."lma_declaration_sessions" enable row level security;


  create table "public"."lma_declarations" (
    "id" text not null,
    "waste_stream_number" text not null,
    "period" text not null,
    "transporters" text[] not null,
    "total_weight" bigint not null,
    "total_shipments" bigint not null,
    "created_at" timestamp with time zone not null default now()
      );


alter table "public"."lma_declarations" enable row level security;


  create table "public"."monthly_waste_declaration_jobs" (
    "id" uuid not null,
    "job_type" text not null,
    "year_month" text not null,
    "status" text not null,
    "created_at" timestamp with time zone not null,
    "fulfilled_at" timestamp with time zone
      );


alter table "public"."monthly_waste_declaration_jobs" enable row level security;

CREATE UNIQUE INDEX lma_declaration_sessions_pkey ON public.lma_declaration_sessions USING btree (id);

CREATE UNIQUE INDEX lma_declarations_pkey ON public.lma_declarations USING btree (id);

CREATE UNIQUE INDEX monthly_waste_declaration_jobs_pkey ON public.monthly_waste_declaration_jobs USING btree (id);

alter table "public"."lma_declaration_sessions" add constraint "lma_declaration_sessions_pkey" PRIMARY KEY using index "lma_declaration_sessions_pkey";

alter table "public"."lma_declarations" add constraint "lma_declarations_pkey" PRIMARY KEY using index "lma_declarations_pkey";

alter table "public"."monthly_waste_declaration_jobs" add constraint "monthly_waste_declaration_jobs_pkey" PRIMARY KEY using index "monthly_waste_declaration_jobs_pkey";

alter table "public"."lma_declarations" add constraint "lma_declarations_waste_stream_number_fkey" FOREIGN KEY (waste_stream_number) REFERENCES public.waste_streams(number) not valid;

alter table "public"."lma_declarations" validate constraint "lma_declarations_waste_stream_number_fkey";

set check_function_bodies = off;

CREATE OR REPLACE FUNCTION public.custom_access_token_hook(event jsonb)
 RETURNS jsonb
 LANGUAGE plpgsql
 STABLE
AS $function$declare
  claims jsonb;
  roles jsonb;
begin
  -- Get all roles for the user as a JSON array
  select jsonb_agg(role)
  into roles
  from public.user_roles
  where user_id = (event->>'user_id')::uuid;

  -- Fallback to an empty array if no roles are found
  if roles is null then
    roles := '[]'::jsonb;
  end if;

  -- Copy existing claims
  claims := event->'claims';

  -- Inject roles into the claims
  claims := jsonb_set(claims, '{user_roles}', roles);

  -- Inject updated claims into the event
  event := jsonb_set(event, '{claims}', claims);

  return event;
end;$function$
;

CREATE OR REPLACE FUNCTION public.handle_new_user()
 RETURNS trigger
 LANGUAGE plpgsql
 SECURITY DEFINER
AS $function$declare
  role text;
begin
  -- Insert basic profile
  insert into public.profiles (id, first_name, last_name)
  values (
    new.id,
    new.raw_user_meta_data->>'first_name',
    new.raw_user_meta_data->>'last_name'
  );

  -- Insert roles if present and valid
  if jsonb_typeof(new.raw_user_meta_data->'roles') = 'array' then
    for role in select jsonb_array_elements_text(new.raw_user_meta_data->'roles')
    loop
      insert into public.user_roles (user_id, role)
      values (new.id, role::public.app_roles)
      on conflict do nothing;
    end loop;
  end if;

  return new;
end$function$
;

grant delete on table "public"."lma_declaration_sessions" to "anon";

grant insert on table "public"."lma_declaration_sessions" to "anon";

grant references on table "public"."lma_declaration_sessions" to "anon";

grant select on table "public"."lma_declaration_sessions" to "anon";

grant trigger on table "public"."lma_declaration_sessions" to "anon";

grant truncate on table "public"."lma_declaration_sessions" to "anon";

grant update on table "public"."lma_declaration_sessions" to "anon";

grant delete on table "public"."lma_declaration_sessions" to "authenticated";

grant insert on table "public"."lma_declaration_sessions" to "authenticated";

grant references on table "public"."lma_declaration_sessions" to "authenticated";

grant select on table "public"."lma_declaration_sessions" to "authenticated";

grant trigger on table "public"."lma_declaration_sessions" to "authenticated";

grant truncate on table "public"."lma_declaration_sessions" to "authenticated";

grant update on table "public"."lma_declaration_sessions" to "authenticated";

grant delete on table "public"."lma_declaration_sessions" to "service_role";

grant insert on table "public"."lma_declaration_sessions" to "service_role";

grant references on table "public"."lma_declaration_sessions" to "service_role";

grant select on table "public"."lma_declaration_sessions" to "service_role";

grant trigger on table "public"."lma_declaration_sessions" to "service_role";

grant truncate on table "public"."lma_declaration_sessions" to "service_role";

grant update on table "public"."lma_declaration_sessions" to "service_role";

grant delete on table "public"."lma_declarations" to "anon";

grant insert on table "public"."lma_declarations" to "anon";

grant references on table "public"."lma_declarations" to "anon";

grant select on table "public"."lma_declarations" to "anon";

grant trigger on table "public"."lma_declarations" to "anon";

grant truncate on table "public"."lma_declarations" to "anon";

grant update on table "public"."lma_declarations" to "anon";

grant delete on table "public"."lma_declarations" to "authenticated";

grant insert on table "public"."lma_declarations" to "authenticated";

grant references on table "public"."lma_declarations" to "authenticated";

grant select on table "public"."lma_declarations" to "authenticated";

grant trigger on table "public"."lma_declarations" to "authenticated";

grant truncate on table "public"."lma_declarations" to "authenticated";

grant update on table "public"."lma_declarations" to "authenticated";

grant delete on table "public"."lma_declarations" to "service_role";

grant insert on table "public"."lma_declarations" to "service_role";

grant references on table "public"."lma_declarations" to "service_role";

grant select on table "public"."lma_declarations" to "service_role";

grant trigger on table "public"."lma_declarations" to "service_role";

grant truncate on table "public"."lma_declarations" to "service_role";

grant update on table "public"."lma_declarations" to "service_role";

grant delete on table "public"."monthly_waste_declaration_jobs" to "anon";

grant insert on table "public"."monthly_waste_declaration_jobs" to "anon";

grant references on table "public"."monthly_waste_declaration_jobs" to "anon";

grant select on table "public"."monthly_waste_declaration_jobs" to "anon";

grant trigger on table "public"."monthly_waste_declaration_jobs" to "anon";

grant truncate on table "public"."monthly_waste_declaration_jobs" to "anon";

grant update on table "public"."monthly_waste_declaration_jobs" to "anon";

grant delete on table "public"."monthly_waste_declaration_jobs" to "authenticated";

grant insert on table "public"."monthly_waste_declaration_jobs" to "authenticated";

grant references on table "public"."monthly_waste_declaration_jobs" to "authenticated";

grant select on table "public"."monthly_waste_declaration_jobs" to "authenticated";

grant trigger on table "public"."monthly_waste_declaration_jobs" to "authenticated";

grant truncate on table "public"."monthly_waste_declaration_jobs" to "authenticated";

grant update on table "public"."monthly_waste_declaration_jobs" to "authenticated";

grant delete on table "public"."monthly_waste_declaration_jobs" to "service_role";

grant insert on table "public"."monthly_waste_declaration_jobs" to "service_role";

grant references on table "public"."monthly_waste_declaration_jobs" to "service_role";

grant select on table "public"."monthly_waste_declaration_jobs" to "service_role";

grant trigger on table "public"."monthly_waste_declaration_jobs" to "service_role";

grant truncate on table "public"."monthly_waste_declaration_jobs" to "service_role";

grant update on table "public"."monthly_waste_declaration_jobs" to "service_role";
