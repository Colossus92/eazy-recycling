alter table "public"."transports" add column "display_number" text not null;

CREATE UNIQUE INDEX transports_display_number_key ON public.transports USING btree (display_number);

CREATE UNIQUE INDEX transports_pkey ON public.transports USING btree (id);

alter table "public"."transports" add constraint "transports_pkey" PRIMARY KEY using index "transports_pkey";

alter table "public"."transports" add constraint "transports_display_number_key" UNIQUE using index "transports_display_number_key";

set check_function_bodies = off;

CREATE OR REPLACE FUNCTION public.ensure_year_sequence(year_prefix text)
 RETURNS text
 LANGUAGE plpgsql
AS $function$
DECLARE
  seq_name TEXT := format('transport_seq_%s', year_prefix);
  seq_exists BOOLEAN;
BEGIN
  SELECT EXISTS (
    SELECT 1
    FROM pg_class
    WHERE relname = seq_name
  ) INTO seq_exists;

  IF NOT seq_exists THEN
    EXECUTE format('CREATE SEQUENCE %I START 1 INCREMENT 1', seq_name);
  END IF;

  RETURN seq_name;
END;
$function$
;

CREATE OR REPLACE FUNCTION public.generate_display_number()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
DECLARE
  year_prefix TEXT := TO_CHAR(NOW(), 'YY');
  seq_name TEXT;
  next_val INT;
BEGIN
  seq_name := ensure_year_sequence(year_prefix);
  EXECUTE format('SELECT nextval(''%I'')', seq_name) INTO next_val;

  -- Always override input
  NEW.display_number := year_prefix || '-' || LPAD(next_val::TEXT, 6, '0');
  RETURN NEW;
END;
$function$
;

CREATE OR REPLACE FUNCTION public.prevent_display_number_update()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
BEGIN
  IF NEW.display_number IS DISTINCT FROM OLD.display_number THEN
    RAISE EXCEPTION 'display_number cannot be changed';
  END IF;
  RETURN NEW;
END;
$function$
;

CREATE TRIGGER lock_display_number BEFORE UPDATE ON public.transports FOR EACH ROW EXECUTE FUNCTION prevent_display_number_update();

CREATE TRIGGER set_display_number BEFORE INSERT ON public.transports FOR EACH ROW EXECUTE FUNCTION generate_display_number();


