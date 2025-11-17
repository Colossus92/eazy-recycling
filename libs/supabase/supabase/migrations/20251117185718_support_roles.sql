
  create table "public"."company_roles" (
    "company_id" uuid not null,
    "roles" text not null
      );


alter table "public"."trucks" add column "carrier_party_id" uuid;

CREATE INDEX idx_company_roles_company_id ON public.company_roles USING btree (company_id);

alter table "public"."company_roles" add constraint "fk_company_roles_company_id" FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE not valid;

alter table "public"."company_roles" validate constraint "fk_company_roles_company_id";

alter table "public"."trucks" add constraint "trucks_carrier_party_id_fkey" FOREIGN KEY (carrier_party_id) REFERENCES public.companies(id) not valid;

alter table "public"."trucks" validate constraint "trucks_carrier_party_id_fkey";

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

grant delete on table "public"."company_roles" to "anon";

grant insert on table "public"."company_roles" to "anon";

grant references on table "public"."company_roles" to "anon";

grant select on table "public"."company_roles" to "anon";

grant trigger on table "public"."company_roles" to "anon";

grant truncate on table "public"."company_roles" to "anon";

grant update on table "public"."company_roles" to "anon";

grant delete on table "public"."company_roles" to "authenticated";

grant insert on table "public"."company_roles" to "authenticated";

grant references on table "public"."company_roles" to "authenticated";

grant select on table "public"."company_roles" to "authenticated";

grant trigger on table "public"."company_roles" to "authenticated";

grant truncate on table "public"."company_roles" to "authenticated";

grant update on table "public"."company_roles" to "authenticated";

grant delete on table "public"."company_roles" to "service_role";

grant insert on table "public"."company_roles" to "service_role";

grant references on table "public"."company_roles" to "service_role";

grant select on table "public"."company_roles" to "service_role";

grant trigger on table "public"."company_roles" to "service_role";

grant truncate on table "public"."company_roles" to "service_role";

grant update on table "public"."company_roles" to "service_role";


