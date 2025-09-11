set check_function_bodies = off;

CREATE OR REPLACE FUNCTION public.handle_updated_user()
 RETURNS trigger
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public', 'pg_catalog'
AS $function$declare
  role text;
begin
  -- Update profile
  update public.profiles
  set first_name = new.raw_user_meta_data->>'first_name',
      last_name = new.raw_user_meta_data->>'last_name'
  where id = new.id;

  -- Replace roles
  if jsonb_typeof(new.raw_user_meta_data->'roles') = 'array' then
    delete from public.user_roles where user_id = new.id;

    for role in select jsonb_array_elements_text(new.raw_user_meta_data->'roles')
    loop
      insert into public.user_roles (user_id, role)
      values (new.id, role::public.app_roles)
      on conflict do nothing;
    end loop;
  end if;

  return new;
end;$function$
;


