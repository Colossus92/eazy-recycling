

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
    "country" character varying,
    "processor_id" "text"
);


ALTER TABLE "public"."companies" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."company_branches" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "company_id" "uuid" NOT NULL,
    "street_name" "text" NOT NULL,
    "postal_code" "text" NOT NULL,
    "city" "text" NOT NULL,
    "updated_at" timestamp with time zone DEFAULT "now"(),
    "building_name" "text",
    "building_number" "text" NOT NULL,
    "country" "text"
);


ALTER TABLE "public"."company_branches" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."eural" (
    "code" "text" NOT NULL,
    "description" "text" NOT NULL
);


ALTER TABLE "public"."eural" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."goods_items" (
    "net_net_weight" integer NOT NULL,
    "quantity" integer NOT NULL,
    "id" bigint NOT NULL,
    "unit" "text",
    "waste_stream_number" "text" NOT NULL
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



CREATE TABLE IF NOT EXISTS "public"."pickup_locations" (
    "id" "text" NOT NULL,
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "location_type" "text" NOT NULL,
    "building_number" "text",
    "building_number_addition" "text",
    "country" "text",
    "proximity_description" "text",
    "city" "text",
    "postal_code" "text",
    "street_name" "text",
    "company_id" "uuid"
);


ALTER TABLE "public"."pickup_locations" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."processing_methods" (
    "code" "text" NOT NULL,
    "description" "text" NOT NULL
);


ALTER TABLE "public"."processing_methods" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."profiles" (
    "id" "uuid" NOT NULL,
    "first_name" "text" NOT NULL,
    "last_name" "text" NOT NULL
);


ALTER TABLE "public"."profiles" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."signatures" (
    "transport_id" "uuid" NOT NULL,
    "consignor_email" "text",
    "consignor_signed_at" timestamp with time zone,
    "pickup_email" "text",
    "pickup_signed_at" timestamp with time zone,
    "carrier_email" "text",
    "carrier_signed_at" timestamp with time zone,
    "consignee_email" "text",
    "consignee_signed_at" timestamp with time zone
);


ALTER TABLE "public"."signatures" OWNER TO "postgres";


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
    "id" "uuid" NOT NULL,
    "delivery_location_id" character varying(255),
    "pickup_location_id" character varying(255),
    "transport_type" "text",
    "truck_id" character varying(255),
    "display_number" "text" NOT NULL,
    "container_id" "uuid",
    "note" "text",
    "container_operation" "text",
    "sequence_number" smallint NOT NULL,
    "transport_hours" numeric(3,1) DEFAULT NULL::numeric,
    "consignor_classification" smallint,
    "goods_item_id" bigint,
    CONSTRAINT "transports_transport_hours_check" CHECK ((("transport_hours" IS NULL) OR ("transport_hours" >= (0)::numeric)))
);


ALTER TABLE "public"."transports" OWNER TO "postgres";


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


CREATE TABLE IF NOT EXISTS "public"."waste_streams" (
    "number" "text" NOT NULL,
    "name" "text" NOT NULL,
    "eural_code" "text" NOT NULL,
    "processing_method_code" "text" NOT NULL,
    "waste_collection_type" "text" NOT NULL,
    "pickup_location_id" "text",
    "consignor_party_id" "uuid" NOT NULL,
    "pickup_party_id" "uuid" NOT NULL,
    "dealer_party_id" "uuid",
    "collector_party_id" "uuid",
    "broker_party_id" "uuid",
    "processor_party_id" "text",
    "last_activity_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "status" "text" DEFAULT 'DRAFT'::"text" NOT NULL
);


ALTER TABLE "public"."waste_streams" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."weight_ticket_lines" (
    "weight_ticket_id" bigint NOT NULL,
    "waste_stream_number" "text" DEFAULT "now"() NOT NULL,
    "weight_value" numeric NOT NULL,
    "weight_unit" "text" NOT NULL
);


ALTER TABLE "public"."weight_ticket_lines" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."weight_tickets" (
    "id" bigint NOT NULL,
    "consignor_party_id" "uuid" NOT NULL,
    "carrier_party_id" "uuid",
    "truck_license_plate" character varying,
    "reclamation" "text",
    "note" "text",
    "status" "text" NOT NULL,
    "created_at" timestamp with time zone NOT NULL,
    "updated_at" timestamp with time zone,
    "weighted_at" timestamp with time zone,
    "cancellation_reason" "text"
);


ALTER TABLE "public"."weight_tickets" OWNER TO "postgres";


ALTER TABLE "public"."weight_tickets" ALTER COLUMN "id" ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME "public"."weight_tickets_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);



ALTER TABLE ONLY "public"."company_branches"
    ADD CONSTRAINT "branches_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."companies"
    ADD CONSTRAINT "companies_chamber_of_commerce_id_key" UNIQUE ("chamber_of_commerce_id");



ALTER TABLE ONLY "public"."companies"
    ADD CONSTRAINT "companies_processor_id_key" UNIQUE ("processor_id");



ALTER TABLE ONLY "public"."companies"
    ADD CONSTRAINT "companies_vihb_number_key" UNIQUE ("vihb_id");



ALTER TABLE ONLY "public"."companies"
    ADD CONSTRAINT "company_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."eural"
    ADD CONSTRAINT "eural_pkey" PRIMARY KEY ("code");



ALTER TABLE ONLY "public"."goods_items"
    ADD CONSTRAINT "goods_items_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."pickup_locations"
    ADD CONSTRAINT "pickup_locations_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."processing_methods"
    ADD CONSTRAINT "processing_methods_pkey" PRIMARY KEY ("code");



ALTER TABLE ONLY "public"."profiles"
    ADD CONSTRAINT "profiles_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."signatures"
    ADD CONSTRAINT "signatures_pkey" PRIMARY KEY ("transport_id");



ALTER TABLE ONLY "public"."transports"
    ADD CONSTRAINT "transports_display_number_key" UNIQUE ("display_number");



ALTER TABLE ONLY "public"."transports"
    ADD CONSTRAINT "transports_pkey" PRIMARY KEY ("id");



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



ALTER TABLE ONLY "public"."waste_streams"
    ADD CONSTRAINT "waste_streams_pkey" PRIMARY KEY ("number");



ALTER TABLE ONLY "public"."weight_tickets"
    ADD CONSTRAINT "weight_tickets_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."company_branches"
    ADD CONSTRAINT "branches_company_id_fkey" FOREIGN KEY ("company_id") REFERENCES "public"."companies"("id") ON UPDATE CASCADE ON DELETE CASCADE;



ALTER TABLE ONLY "public"."transports"
    ADD CONSTRAINT "fk2lgi3xage1g6g2daa75jhnw1c" FOREIGN KEY ("truck_id") REFERENCES "public"."trucks"("license_plate");



ALTER TABLE ONLY "public"."transports"
    ADD CONSTRAINT "fk9vtcum4wow21lygabe1baemab" FOREIGN KEY ("carrier_party_id") REFERENCES "public"."companies"("id");



ALTER TABLE ONLY "public"."transports"
    ADD CONSTRAINT "fkpfpjw6nu1nix3orr0vc2c2na6" FOREIGN KEY ("consignor_party_id") REFERENCES "public"."companies"("id");



ALTER TABLE ONLY "public"."goods_items"
    ADD CONSTRAINT "goods_items_waste_stream_number_fkey" FOREIGN KEY ("waste_stream_number") REFERENCES "public"."waste_streams"("number");



ALTER TABLE ONLY "public"."profiles"
    ADD CONSTRAINT "profiles_id_fkey" FOREIGN KEY ("id") REFERENCES "auth"."users"("id") ON UPDATE CASCADE ON DELETE CASCADE;



ALTER TABLE ONLY "public"."signatures"
    ADD CONSTRAINT "signatures_transport_id_fkey" FOREIGN KEY ("transport_id") REFERENCES "public"."transports"("id");



ALTER TABLE ONLY "public"."transports"
    ADD CONSTRAINT "transports_container_id_fkey" FOREIGN KEY ("container_id") REFERENCES "public"."waste_containers"("uuid");



ALTER TABLE ONLY "public"."transports"
    ADD CONSTRAINT "transports_delivery_location_id_fkey" FOREIGN KEY ("delivery_location_id") REFERENCES "public"."pickup_locations"("id");



ALTER TABLE ONLY "public"."transports"
    ADD CONSTRAINT "transports_driver_id_fkey" FOREIGN KEY ("driver_id") REFERENCES "public"."profiles"("id");



ALTER TABLE ONLY "public"."transports"
    ADD CONSTRAINT "transports_goods_item_id_fkey" FOREIGN KEY ("goods_item_id") REFERENCES "public"."goods_items"("id");



ALTER TABLE ONLY "public"."transports"
    ADD CONSTRAINT "transports_pickup_location_id_fkey" FOREIGN KEY ("pickup_location_id") REFERENCES "public"."pickup_locations"("id");



ALTER TABLE ONLY "public"."user_roles"
    ADD CONSTRAINT "user_roles_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "auth"."users"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."waste_containers"
    ADD CONSTRAINT "waste_containers_location_company_id_fkey" FOREIGN KEY ("location_company_id") REFERENCES "public"."companies"("id") ON UPDATE CASCADE ON DELETE SET NULL;



ALTER TABLE ONLY "public"."waste_streams"
    ADD CONSTRAINT "waste_streams_broker_party_id_fkey" FOREIGN KEY ("broker_party_id") REFERENCES "public"."companies"("id");



ALTER TABLE ONLY "public"."waste_streams"
    ADD CONSTRAINT "waste_streams_collector_party_id_fkey" FOREIGN KEY ("collector_party_id") REFERENCES "public"."companies"("id");



ALTER TABLE ONLY "public"."waste_streams"
    ADD CONSTRAINT "waste_streams_consignor_party_id_fkey" FOREIGN KEY ("consignor_party_id") REFERENCES "public"."companies"("id");



ALTER TABLE ONLY "public"."waste_streams"
    ADD CONSTRAINT "waste_streams_dealer_party_id_fkey" FOREIGN KEY ("dealer_party_id") REFERENCES "public"."companies"("id");



ALTER TABLE ONLY "public"."waste_streams"
    ADD CONSTRAINT "waste_streams_eural_code_fkey" FOREIGN KEY ("eural_code") REFERENCES "public"."eural"("code");



ALTER TABLE ONLY "public"."waste_streams"
    ADD CONSTRAINT "waste_streams_pickup_location_id_fkey" FOREIGN KEY ("pickup_location_id") REFERENCES "public"."pickup_locations"("id");



ALTER TABLE ONLY "public"."waste_streams"
    ADD CONSTRAINT "waste_streams_pickup_party_id_fkey" FOREIGN KEY ("pickup_party_id") REFERENCES "public"."companies"("id");



ALTER TABLE ONLY "public"."waste_streams"
    ADD CONSTRAINT "waste_streams_processing_method_code_fkey" FOREIGN KEY ("processing_method_code") REFERENCES "public"."processing_methods"("code");



ALTER TABLE ONLY "public"."waste_streams"
    ADD CONSTRAINT "waste_streams_processor_party_id_fkey" FOREIGN KEY ("processor_party_id") REFERENCES "public"."companies"("processor_id");



ALTER TABLE ONLY "public"."weight_ticket_lines"
    ADD CONSTRAINT "weight_ticket_lines_waste_stream_number_fkey" FOREIGN KEY ("waste_stream_number") REFERENCES "public"."waste_streams"("number");



ALTER TABLE ONLY "public"."weight_ticket_lines"
    ADD CONSTRAINT "weight_ticket_lines_weight_ticket_id_fkey" FOREIGN KEY ("weight_ticket_id") REFERENCES "public"."weight_tickets"("id");



ALTER TABLE ONLY "public"."weight_tickets"
    ADD CONSTRAINT "weight_tickets_carrier_party_id_fkey" FOREIGN KEY ("carrier_party_id") REFERENCES "public"."companies"("id");



ALTER TABLE ONLY "public"."weight_tickets"
    ADD CONSTRAINT "weight_tickets_consignor_party_id_fkey" FOREIGN KEY ("consignor_party_id") REFERENCES "public"."companies"("id");



ALTER TABLE ONLY "public"."weight_tickets"
    ADD CONSTRAINT "weight_tickets_truck_license_plate_fkey" FOREIGN KEY ("truck_license_plate") REFERENCES "public"."trucks"("license_plate");



CREATE POLICY "Allow auth admin to read user roles" ON "public"."user_roles" FOR SELECT TO "supabase_auth_admin" USING (true);



CREATE POLICY "Enable all access for authenticated users only" ON "public"."companies" USING (true) WITH CHECK (true);



CREATE POLICY "Enable all access for authenticated users only" ON "public"."trucks" TO "authenticated" USING (true) WITH CHECK (true);



ALTER TABLE "public"."companies" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."company_branches" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."eural" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."goods_items" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."pickup_locations" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."processing_methods" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."profiles" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."signatures" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."transports" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."trucks" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."user_roles" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."waste_containers" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."waste_streams" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."weight_ticket_lines" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."weight_tickets" ENABLE ROW LEVEL SECURITY;




ALTER PUBLICATION "supabase_realtime" OWNER TO "postgres";





GRANT USAGE ON SCHEMA "public" TO "postgres";
GRANT USAGE ON SCHEMA "public" TO "anon";
GRANT USAGE ON SCHEMA "public" TO "authenticated";
GRANT USAGE ON SCHEMA "public" TO "service_role";
GRANT USAGE ON SCHEMA "public" TO "supabase_auth_admin";


























































































































































































GRANT ALL ON FUNCTION "public"."custom_access_token_hook"("event" "jsonb") TO "service_role";
GRANT ALL ON FUNCTION "public"."custom_access_token_hook"("event" "jsonb") TO "supabase_auth_admin";



GRANT ALL ON FUNCTION "public"."handle_new_user"() TO "anon";
GRANT ALL ON FUNCTION "public"."handle_new_user"() TO "authenticated";
GRANT ALL ON FUNCTION "public"."handle_new_user"() TO "service_role";



GRANT ALL ON FUNCTION "public"."handle_updated_user"() TO "anon";
GRANT ALL ON FUNCTION "public"."handle_updated_user"() TO "authenticated";
GRANT ALL ON FUNCTION "public"."handle_updated_user"() TO "service_role";


















GRANT ALL ON TABLE "public"."companies" TO "anon";
GRANT ALL ON TABLE "public"."companies" TO "authenticated";
GRANT ALL ON TABLE "public"."companies" TO "service_role";



GRANT ALL ON TABLE "public"."company_branches" TO "anon";
GRANT ALL ON TABLE "public"."company_branches" TO "authenticated";
GRANT ALL ON TABLE "public"."company_branches" TO "service_role";



GRANT ALL ON TABLE "public"."eural" TO "anon";
GRANT ALL ON TABLE "public"."eural" TO "authenticated";
GRANT ALL ON TABLE "public"."eural" TO "service_role";



GRANT ALL ON TABLE "public"."goods_items" TO "anon";
GRANT ALL ON TABLE "public"."goods_items" TO "authenticated";
GRANT ALL ON TABLE "public"."goods_items" TO "service_role";



GRANT ALL ON SEQUENCE "public"."goods_items_id_seq" TO "anon";
GRANT ALL ON SEQUENCE "public"."goods_items_id_seq" TO "authenticated";
GRANT ALL ON SEQUENCE "public"."goods_items_id_seq" TO "service_role";



GRANT ALL ON TABLE "public"."pickup_locations" TO "anon";
GRANT ALL ON TABLE "public"."pickup_locations" TO "authenticated";
GRANT ALL ON TABLE "public"."pickup_locations" TO "service_role";



GRANT ALL ON TABLE "public"."processing_methods" TO "anon";
GRANT ALL ON TABLE "public"."processing_methods" TO "authenticated";
GRANT ALL ON TABLE "public"."processing_methods" TO "service_role";



GRANT ALL ON TABLE "public"."profiles" TO "anon";
GRANT ALL ON TABLE "public"."profiles" TO "authenticated";
GRANT ALL ON TABLE "public"."profiles" TO "service_role";



GRANT ALL ON TABLE "public"."signatures" TO "anon";
GRANT ALL ON TABLE "public"."signatures" TO "authenticated";
GRANT ALL ON TABLE "public"."signatures" TO "service_role";



GRANT ALL ON SEQUENCE "public"."transport_seq_25" TO "anon";
GRANT ALL ON SEQUENCE "public"."transport_seq_25" TO "authenticated";
GRANT ALL ON SEQUENCE "public"."transport_seq_25" TO "service_role";



GRANT ALL ON TABLE "public"."transports" TO "anon";
GRANT ALL ON TABLE "public"."transports" TO "authenticated";
GRANT ALL ON TABLE "public"."transports" TO "service_role";



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



GRANT ALL ON TABLE "public"."waste_streams" TO "anon";
GRANT ALL ON TABLE "public"."waste_streams" TO "authenticated";
GRANT ALL ON TABLE "public"."waste_streams" TO "service_role";



GRANT ALL ON TABLE "public"."weight_ticket_lines" TO "anon";
GRANT ALL ON TABLE "public"."weight_ticket_lines" TO "authenticated";
GRANT ALL ON TABLE "public"."weight_ticket_lines" TO "service_role";



GRANT ALL ON TABLE "public"."weight_tickets" TO "anon";
GRANT ALL ON TABLE "public"."weight_tickets" TO "authenticated";
GRANT ALL ON TABLE "public"."weight_tickets" TO "service_role";



GRANT ALL ON SEQUENCE "public"."weight_tickets_id_seq" TO "anon";
GRANT ALL ON SEQUENCE "public"."weight_tickets_id_seq" TO "authenticated";
GRANT ALL ON SEQUENCE "public"."weight_tickets_id_seq" TO "service_role";



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































--
-- Dumped schema changes for auth and storage
--

CREATE OR REPLACE TRIGGER "on_auth_user_created" AFTER INSERT ON "auth"."users" FOR EACH ROW EXECUTE FUNCTION "public"."handle_new_user"();



CREATE OR REPLACE TRIGGER "on_auth_user_updated" AFTER UPDATE ON "auth"."users" FOR EACH ROW EXECUTE FUNCTION "public"."handle_updated_user"();



CREATE POLICY "Authenticated can list and upload bd9ltp_0" ON "storage"."objects" FOR SELECT TO "authenticated" USING (("bucket_id" = 'waybills'::"text"));



CREATE POLICY "Authenticated can list and upload bd9ltp_1" ON "storage"."objects" FOR INSERT TO "authenticated" WITH CHECK (("bucket_id" = 'waybills'::"text"));



CREATE POLICY "Authenticated can list and upload bd9ltp_2" ON "storage"."objects" FOR UPDATE TO "authenticated" USING (("bucket_id" = 'waybills'::"text"));


