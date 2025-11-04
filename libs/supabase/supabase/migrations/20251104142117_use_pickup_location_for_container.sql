alter table "public"."waste_containers" add column "location_id" text;

alter table "public"."waste_containers" add constraint "waste_containers_location_id_fkey" FOREIGN KEY (location_id) REFERENCES public.pickup_locations(id) not valid;

alter table "public"."waste_containers" validate constraint "waste_containers_location_id_fkey";

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

CREATE TRIGGER tr_check_filters BEFORE INSERT OR UPDATE ON realtime.subscription FOR EACH ROW EXECUTE FUNCTION realtime.subscription_check_filters();


