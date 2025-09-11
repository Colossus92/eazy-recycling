

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;


CREATE EXTENSION IF NOT EXISTS "pg_net" WITH SCHEMA "extensions";






CREATE EXTENSION IF NOT EXISTS "pgsodium";






COMMENT ON SCHEMA "public" IS 'standard public schema';



CREATE EXTENSION IF NOT EXISTS "pg_graphql" WITH SCHEMA "graphql";






CREATE EXTENSION IF NOT EXISTS "pg_stat_statements" WITH SCHEMA "extensions";






CREATE EXTENSION IF NOT EXISTS "pgcrypto" WITH SCHEMA "extensions";






CREATE EXTENSION IF NOT EXISTS "pgjwt" WITH SCHEMA "extensions";






CREATE EXTENSION IF NOT EXISTS "supabase_vault" WITH SCHEMA "vault";






CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA "extensions";






CREATE TYPE "public"."app_roles" AS ENUM (
    'admin',
    'planner',
    'chauffeur'
);


ALTER TYPE "public"."app_roles" OWNER TO "postgres";


CREATE TYPE "public"."container_operations" AS ENUM (
    'EXCHANGE',
    'EMPTY',
    'PICKUP',
    'DELIVERY',
    'WAYBILL'
);


ALTER TYPE "public"."container_operations" OWNER TO "postgres";


CREATE TYPE "public"."units" AS ENUM (
    'KG',
    'TON',
    'LITER',
    'M3',
    'PIECES',
    'CONTAINER'
);


ALTER TYPE "public"."units" OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."custom_access_token_hook"("event" "jsonb") RETURNS "jsonb"
    LANGUAGE "plpgsql" STABLE
    AS $$declare
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
end;$$;


ALTER FUNCTION "public"."custom_access_token_hook"("event" "jsonb") OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."ensure_year_sequence"("year_prefix" "text") RETURNS "text"
    LANGUAGE "plpgsql"
    AS $$
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
$$;


ALTER FUNCTION "public"."ensure_year_sequence"("year_prefix" "text") OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."generate_display_number"() RETURNS "trigger"
    LANGUAGE "plpgsql"
    AS $$
DECLARE
  year_prefix TEXT := TO_CHAR(NOW(), 'YY');
  seq_name TEXT;
  next_val INT;
BEGIN
  seq_name := ensure_year_sequence(year_prefix);
  EXECUTE format('SELECT nextval(''%I'')', seq_name) INTO next_val;

  -- Always override input
  NEW.display_number := year_prefix || '-' || LPAD(next_val::TEXT, 4, '0');
  RETURN NEW;
END;
$$;


ALTER FUNCTION "public"."generate_display_number"() OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."handle_new_user"() RETURNS "trigger"
    LANGUAGE "plpgsql" SECURITY DEFINER
    AS $$declare
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
end$$;


ALTER FUNCTION "public"."handle_new_user"() OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."handle_updated_user"() RETURNS "trigger"
    LANGUAGE "plpgsql" SECURITY DEFINER
    SET "search_path" TO 'public', 'pg_catalog'
    AS $$declare
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
end;$$;


ALTER FUNCTION "public"."handle_updated_user"() OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."prevent_display_number_update"() RETURNS "trigger"
    LANGUAGE "plpgsql"
    AS $$
BEGIN
  IF NEW.display_number IS DISTINCT FROM OLD.display_number THEN
    RAISE EXCEPTION 'display_number cannot be changed';
  END IF;
  RETURN NEW;
END;
$$;


ALTER FUNCTION "public"."prevent_display_number_update"() OWNER TO "postgres";

SET default_tablespace = '';

SET default_table_access_method = "heap";


CREATE TABLE IF NOT EXISTS "public"."companies" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "name" character varying NOT NULL,
    "chamber_of_commerce_id" character varying,
    "street_name" character varying NOT NULL,
    "postal_code" character varying NOT NULL,
    "city" character varying NOT NULL,
    "vihb_id" character varying,
    "updated_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "building_name" character varying,
    "building_number" character varying,
    "country" character varying
);


ALTER TABLE "public"."companies" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."goods" (
    "goods_item_id" bigint,
    "updated_at" timestamp(6) without time zone NOT NULL,
    "consignee_party_id" "uuid",
    "pickup_party_id" "uuid",
    "uuid" "uuid" NOT NULL,
    "id" character varying(255)
);


ALTER TABLE "public"."goods" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."goods_items" (
    "net_net_weight" integer NOT NULL,
    "quantity" integer NOT NULL,
    "id" bigint NOT NULL,
    "eural_code" character varying(255),
    "name" character varying(255),
    "unit" character varying(255),
    "waste_stream_number" character varying(255)
);


ALTER TABLE "public"."goods_items" OWNER TO "postgres";


ALTER TABLE "public"."goods_items" ALTER COLUMN "id" ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME "public"."goods_items_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);



CREATE TABLE IF NOT EXISTS "public"."locations" (
    "building_name" character varying(255),
    "building_number" character varying(255),
    "city" character varying(255),
    "country" character varying(255),
    "description" character varying(255),
    "id" character varying(255) NOT NULL,
    "location_type_code" character varying(255),
    "postal_code" character varying(255),
    "street_name" character varying(255)
);


ALTER TABLE "public"."locations" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."profiles" (
    "id" "uuid" NOT NULL,
    "first_name" "text" NOT NULL,
    "last_name" "text" NOT NULL
);


ALTER TABLE "public"."profiles" OWNER TO "postgres";


CREATE SEQUENCE IF NOT EXISTS "public"."transport_seq_25"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE "public"."transport_seq_25" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."transports" (
    "delivery_date_time" timestamp(6) without time zone,
    "pickup_date_time" timestamp(6) without time zone,
    "updated_at" timestamp(6) without time zone NOT NULL,
    "carrier_party_id" "uuid",
    "consignor_party_id" "uuid",
    "driver_id" "uuid",
    "goods_id" "uuid",
    "id" "uuid" NOT NULL,
    "delivery_location_id" character varying(255),
    "pickup_location_id" character varying(255),
    "transport_type" "text",
    "truck_id" character varying(255),
    "display_number" "text" NOT NULL,
    "container_id" "uuid",
    "delivery_company_id" "uuid",
    "pickup_company_id" "uuid",
    "note" "text",
    "container_operation" "text"
);


ALTER TABLE "public"."transports" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."trips" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "origin_company_id" "uuid",
    "destination_company_id" "uuid",
    "truck_license_plate" character varying,
    "driver_id" "uuid",
    "waste_code" character varying NOT NULL,
    "waste_name" character varying NOT NULL,
    "waste_amount" numeric NOT NULL,
    "waste_unit" "public"."units",
    "weight_ticket_number" character varying,
    "start_datetime" timestamp with time zone,
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL
);


ALTER TABLE "public"."trips" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."trucks" (
    "license_plate" character varying NOT NULL,
    "brand" character varying NOT NULL,
    "model" character varying NOT NULL,
    "updated_at" timestamp with time zone DEFAULT "now"() NOT NULL
);


ALTER TABLE "public"."trucks" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."user_roles" (
    "id" bigint NOT NULL,
    "user_id" "uuid" NOT NULL,
    "role" "public"."app_roles" NOT NULL
);


ALTER TABLE "public"."user_roles" OWNER TO "postgres";


ALTER TABLE "public"."user_roles" ALTER COLUMN "id" ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME "public"."user_roles_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);



CREATE TABLE IF NOT EXISTS "public"."waste_containers" (
    "uuid" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "id" "text" NOT NULL,
    "type" "text",
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "updated_at" timestamp with time zone DEFAULT "now"(),
    "location_company_id" "uuid",
    "street_name" "text",
    "postal_code" "text",
    "city" "text",
    "building_number" "text",
    "building_name" "text",
    "country" "text",
    "notes" "text"
);


ALTER TABLE "public"."waste_containers" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."waybills" (
    "delivery_date_time" timestamp(6) without time zone,
    "goods_item_id" bigint,
    "pickup_date_time" timestamp(6) without time zone,
    "updated_at" timestamp(6) without time zone NOT NULL,
    "carrier_party_id" "uuid",
    "consignee_party_id" "uuid",
    "consignor_party_id" "uuid",
    "pickup_party_id" "uuid",
    "uuid" "uuid" NOT NULL,
    "delivery_location_id" character varying(255),
    "id" character varying(255),
    "license_plate" character varying(255),
    "note" character varying(255),
    "pickup_location_id" character varying(255)
);


ALTER TABLE "public"."waybills" OWNER TO "postgres";


ALTER TABLE ONLY "public"."companies"
    ADD CONSTRAINT "companies_chamber_of_commerce_id_key" UNIQUE ("chamber_of_commerce_id");



ALTER TABLE ONLY "public"."companies"
    ADD CONSTRAINT "companies_vihb_number_key" UNIQUE ("vihb_id");



ALTER TABLE ONLY "public"."companies"
    ADD CONSTRAINT "company_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."goods"
    ADD CONSTRAINT "goods_goods_item_id_key" UNIQUE ("goods_item_id");



ALTER TABLE ONLY "public"."goods_items"
    ADD CONSTRAINT "goods_items_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."goods"
    ADD CONSTRAINT "goods_pkey" PRIMARY KEY ("uuid");



ALTER TABLE ONLY "public"."locations"
    ADD CONSTRAINT "locations_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."locations"
    ADD CONSTRAINT "locations_postal_building_unique" UNIQUE ("postal_code", "building_number");



ALTER TABLE ONLY "public"."profiles"
    ADD CONSTRAINT "profiles_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."transports"
    ADD CONSTRAINT "transports_display_number_key" UNIQUE ("display_number");



ALTER TABLE ONLY "public"."transports"
    ADD CONSTRAINT "transports_goods_id_key" UNIQUE ("goods_id");



ALTER TABLE ONLY "public"."transports"
    ADD CONSTRAINT "transports_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."trips"
    ADD CONSTRAINT "trips_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."trucks"
    ADD CONSTRAINT "trucks_pkey" PRIMARY KEY ("license_plate");



ALTER TABLE ONLY "public"."user_roles"
    ADD CONSTRAINT "user_roles_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."user_roles"
    ADD CONSTRAINT "user_roles_user_id_role_key" UNIQUE ("user_id", "role");



ALTER TABLE ONLY "public"."waste_containers"
    ADD CONSTRAINT "waste_containers_id_key" UNIQUE ("id");



ALTER TABLE ONLY "public"."waste_containers"
    ADD CONSTRAINT "waste_containers_pkey" PRIMARY KEY ("uuid");



ALTER TABLE ONLY "public"."waybills"
    ADD CONSTRAINT "waybills_goods_item_id_key" UNIQUE ("goods_item_id");



ALTER TABLE ONLY "public"."waybills"
    ADD CONSTRAINT "waybills_pkey" PRIMARY KEY ("uuid");



CREATE OR REPLACE TRIGGER "lock_display_number" BEFORE UPDATE ON "public"."transports" FOR EACH ROW EXECUTE FUNCTION "public"."prevent_display_number_update"();



CREATE OR REPLACE TRIGGER "set_display_number" BEFORE INSERT ON "public"."transports" FOR EACH ROW EXECUTE FUNCTION "public"."generate_display_number"();



ALTER TABLE ONLY "public"."transports"
    ADD CONSTRAINT "fk2lgi3xage1g6g2daa75jhnw1c" FOREIGN KEY ("truck_id") REFERENCES "public"."trucks"("license_plate");



ALTER TABLE ONLY "public"."waybills"
    ADD CONSTRAINT "fk3x2ksx5hynkablsj7k46cg1qs" FOREIGN KEY ("pickup_party_id") REFERENCES "public"."companies"("id");



ALTER TABLE ONLY "public"."waybills"
    ADD CONSTRAINT "fk6wh6ln69550kuiji853lvwitl" FOREIGN KEY ("delivery_location_id") REFERENCES "public"."locations"("id");



ALTER TABLE ONLY "public"."goods"
    ADD CONSTRAINT "fk8b4xllidyrhdclt6pjqo5k5aq" FOREIGN KEY ("goods_item_id") REFERENCES "public"."goods_items"("id");



ALTER TABLE ONLY "public"."transports"
    ADD CONSTRAINT "fk8jrwx32tgeb6xn4e9jg5mueuj" FOREIGN KEY ("goods_id") REFERENCES "public"."goods"("uuid");



ALTER TABLE ONLY "public"."transports"
    ADD CONSTRAINT "fk9vtcum4wow21lygabe1baemab" FOREIGN KEY ("carrier_party_id") REFERENCES "public"."companies"("id");



ALTER TABLE ONLY "public"."waybills"
    ADD CONSTRAINT "fkexb5sdt0oqb9a6ehk1f05krmv" FOREIGN KEY ("carrier_party_id") REFERENCES "public"."companies"("id");



ALTER TABLE ONLY "public"."waybills"
    ADD CONSTRAINT "fkh7c49u3crn1swqeoton0umk4x" FOREIGN KEY ("pickup_location_id") REFERENCES "public"."locations"("id");



ALTER TABLE ONLY "public"."waybills"
    ADD CONSTRAINT "fkhaxs4v1au4fk8sahm9map6j49" FOREIGN KEY ("goods_item_id") REFERENCES "public"."goods_items"("id");



ALTER TABLE ONLY "public"."goods"
    ADD CONSTRAINT "fkjxo4a2n1ffa1vi323ixkm0efl" FOREIGN KEY ("pickup_party_id") REFERENCES "public"."companies"("id");



ALTER TABLE ONLY "public"."goods"
    ADD CONSTRAINT "fkmmq89gkjejce7jk9b3ifrq85n" FOREIGN KEY ("consignee_party_id") REFERENCES "public"."companies"("id");



ALTER TABLE ONLY "public"."transports"
    ADD CONSTRAINT "fkpfpjw6nu1nix3orr0vc2c2na6" FOREIGN KEY ("consignor_party_id") REFERENCES "public"."companies"("id");



ALTER TABLE ONLY "public"."waybills"
    ADD CONSTRAINT "fksrjknpahp89cmae80n86m0hsy" FOREIGN KEY ("consignee_party_id") REFERENCES "public"."companies"("id");



ALTER TABLE ONLY "public"."waybills"
    ADD CONSTRAINT "fkswgts2ytehbdal35brikeaexc" FOREIGN KEY ("consignor_party_id") REFERENCES "public"."companies"("id");



ALTER TABLE ONLY "public"."profiles"
    ADD CONSTRAINT "profiles_id_fkey" FOREIGN KEY ("id") REFERENCES "auth"."users"("id") ON UPDATE CASCADE ON DELETE CASCADE;



ALTER TABLE ONLY "public"."transports"
    ADD CONSTRAINT "transports_container_id_fkey" FOREIGN KEY ("container_id") REFERENCES "public"."waste_containers"("uuid");



ALTER TABLE ONLY "public"."transports"
    ADD CONSTRAINT "transports_delivery_company_id_fkey" FOREIGN KEY ("delivery_company_id") REFERENCES "public"."companies"("id");



ALTER TABLE ONLY "public"."transports"
    ADD CONSTRAINT "transports_delivery_location_id_fkey" FOREIGN KEY ("delivery_location_id") REFERENCES "public"."locations"("id");



ALTER TABLE ONLY "public"."transports"
    ADD CONSTRAINT "transports_driver_id_fkey" FOREIGN KEY ("driver_id") REFERENCES "public"."profiles"("id");



ALTER TABLE ONLY "public"."transports"
    ADD CONSTRAINT "transports_pickup_company_id_fkey" FOREIGN KEY ("pickup_company_id") REFERENCES "public"."companies"("id");



ALTER TABLE ONLY "public"."transports"
    ADD CONSTRAINT "transports_pickup_location_id_fkey" FOREIGN KEY ("pickup_location_id") REFERENCES "public"."locations"("id");



ALTER TABLE ONLY "public"."trips"
    ADD CONSTRAINT "trips_destination_company_id_fkey" FOREIGN KEY ("destination_company_id") REFERENCES "public"."companies"("id") ON UPDATE RESTRICT ON DELETE SET NULL;



ALTER TABLE ONLY "public"."trips"
    ADD CONSTRAINT "trips_driver_id_fkey1" FOREIGN KEY ("driver_id") REFERENCES "public"."profiles"("id");



ALTER TABLE ONLY "public"."trips"
    ADD CONSTRAINT "trips_origin_company_id_fkey" FOREIGN KEY ("origin_company_id") REFERENCES "public"."companies"("id") ON UPDATE RESTRICT ON DELETE SET NULL;



ALTER TABLE ONLY "public"."trips"
    ADD CONSTRAINT "trips_truck_license_plate_fkey" FOREIGN KEY ("truck_license_plate") REFERENCES "public"."trucks"("license_plate") ON UPDATE RESTRICT ON DELETE RESTRICT;



ALTER TABLE ONLY "public"."user_roles"
    ADD CONSTRAINT "user_roles_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "auth"."users"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."waste_containers"
    ADD CONSTRAINT "waste_containers_location_company_id_fkey" FOREIGN KEY ("location_company_id") REFERENCES "public"."companies"("id") ON UPDATE CASCADE ON DELETE SET NULL;



CREATE POLICY "Allow auth admin to read user roles" ON "public"."user_roles" FOR SELECT TO "supabase_auth_admin" USING (true);



CREATE POLICY "Enable all access for authenticated users only" ON "public"."companies" USING (true) WITH CHECK (true);



CREATE POLICY "Enable all access for authenticated users only" ON "public"."trips" TO "authenticated" USING (true) WITH CHECK (true);



CREATE POLICY "Enable all access for authenticated users only" ON "public"."trucks" TO "authenticated" USING (true) WITH CHECK (true);



ALTER TABLE "public"."companies" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."goods_items" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."profiles" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."trips" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."trucks" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."waste_containers" ENABLE ROW LEVEL SECURITY;




ALTER PUBLICATION "supabase_realtime" OWNER TO "postgres";





GRANT USAGE ON SCHEMA "public" TO "postgres";
GRANT USAGE ON SCHEMA "public" TO "anon";
GRANT USAGE ON SCHEMA "public" TO "authenticated";
GRANT USAGE ON SCHEMA "public" TO "service_role";


























































































































































































GRANT ALL ON FUNCTION "public"."custom_access_token_hook"("event" "jsonb") TO "anon";
GRANT ALL ON FUNCTION "public"."custom_access_token_hook"("event" "jsonb") TO "authenticated";
GRANT ALL ON FUNCTION "public"."custom_access_token_hook"("event" "jsonb") TO "service_role";



GRANT ALL ON FUNCTION "public"."ensure_year_sequence"("year_prefix" "text") TO "anon";
GRANT ALL ON FUNCTION "public"."ensure_year_sequence"("year_prefix" "text") TO "authenticated";
GRANT ALL ON FUNCTION "public"."ensure_year_sequence"("year_prefix" "text") TO "service_role";



GRANT ALL ON FUNCTION "public"."generate_display_number"() TO "anon";
GRANT ALL ON FUNCTION "public"."generate_display_number"() TO "authenticated";
GRANT ALL ON FUNCTION "public"."generate_display_number"() TO "service_role";




GRANT ALL ON FUNCTION "public"."handle_new_user"() TO "anon";
GRANT ALL ON FUNCTION "public"."handle_new_user"() TO "authenticated";
GRANT ALL ON FUNCTION "public"."handle_new_user"() TO "service_role";



GRANT ALL ON FUNCTION "public"."handle_updated_user"() TO "anon";
GRANT ALL ON FUNCTION "public"."handle_updated_user"() TO "authenticated";
GRANT ALL ON FUNCTION "public"."handle_updated_user"() TO "service_role";



GRANT ALL ON FUNCTION "public"."prevent_display_number_update"() TO "anon";
GRANT ALL ON FUNCTION "public"."prevent_display_number_update"() TO "authenticated";
GRANT ALL ON FUNCTION "public"."prevent_display_number_update"() TO "service_role";


















GRANT ALL ON TABLE "public"."companies" TO "anon";
GRANT ALL ON TABLE "public"."companies" TO "authenticated";
GRANT ALL ON TABLE "public"."companies" TO "service_role";



GRANT ALL ON TABLE "public"."goods" TO "anon";
GRANT ALL ON TABLE "public"."goods" TO "authenticated";
GRANT ALL ON TABLE "public"."goods" TO "service_role";



GRANT ALL ON TABLE "public"."goods_items" TO "anon";
GRANT ALL ON TABLE "public"."goods_items" TO "authenticated";
GRANT ALL ON TABLE "public"."goods_items" TO "service_role";



GRANT ALL ON SEQUENCE "public"."goods_items_id_seq" TO "anon";
GRANT ALL ON SEQUENCE "public"."goods_items_id_seq" TO "authenticated";
GRANT ALL ON SEQUENCE "public"."goods_items_id_seq" TO "service_role";



GRANT ALL ON TABLE "public"."locations" TO "anon";
GRANT ALL ON TABLE "public"."locations" TO "authenticated";
GRANT ALL ON TABLE "public"."locations" TO "service_role";



GRANT ALL ON TABLE "public"."profiles" TO "anon";
GRANT ALL ON TABLE "public"."profiles" TO "authenticated";
GRANT ALL ON TABLE "public"."profiles" TO "service_role";



GRANT ALL ON SEQUENCE "public"."transport_seq_25" TO "anon";
GRANT ALL ON SEQUENCE "public"."transport_seq_25" TO "authenticated";
GRANT ALL ON SEQUENCE "public"."transport_seq_25" TO "service_role";



GRANT ALL ON TABLE "public"."transports" TO "anon";
GRANT ALL ON TABLE "public"."transports" TO "authenticated";
GRANT ALL ON TABLE "public"."transports" TO "service_role";



GRANT ALL ON TABLE "public"."trips" TO "anon";
GRANT ALL ON TABLE "public"."trips" TO "authenticated";
GRANT ALL ON TABLE "public"."trips" TO "service_role";



GRANT ALL ON TABLE "public"."trucks" TO "anon";
GRANT ALL ON TABLE "public"."trucks" TO "authenticated";
GRANT ALL ON TABLE "public"."trucks" TO "service_role";



GRANT ALL ON TABLE "public"."user_roles" TO "service_role";
GRANT ALL ON TABLE "public"."user_roles" TO "supabase_auth_admin";



GRANT ALL ON SEQUENCE "public"."user_roles_id_seq" TO "anon";
GRANT ALL ON SEQUENCE "public"."user_roles_id_seq" TO "authenticated";
GRANT ALL ON SEQUENCE "public"."user_roles_id_seq" TO "service_role";



GRANT ALL ON TABLE "public"."waste_containers" TO "anon";
GRANT ALL ON TABLE "public"."waste_containers" TO "authenticated";
GRANT ALL ON TABLE "public"."waste_containers" TO "service_role";



GRANT ALL ON TABLE "public"."waybills" TO "anon";
GRANT ALL ON TABLE "public"."waybills" TO "authenticated";
GRANT ALL ON TABLE "public"."waybills" TO "service_role";



ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON SEQUENCES  TO "postgres";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON SEQUENCES  TO "anon";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON SEQUENCES  TO "authenticated";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON SEQUENCES  TO "service_role";






ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON FUNCTIONS  TO "postgres";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON FUNCTIONS  TO "anon";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON FUNCTIONS  TO "authenticated";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON FUNCTIONS  TO "service_role";






ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON TABLES  TO "postgres";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON TABLES  TO "anon";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON TABLES  TO "authenticated";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON TABLES  TO "service_role";






























RESET ALL;
