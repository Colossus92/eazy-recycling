

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


CREATE SCHEMA IF NOT EXISTS "jobrunr";


ALTER SCHEMA "jobrunr" OWNER TO "postgres";


CREATE EXTENSION IF NOT EXISTS "pg_net" WITH SCHEMA "extensions";






CREATE EXTENSION IF NOT EXISTS "pgsodium";






COMMENT ON SCHEMA "public" IS 'standard public schema';



CREATE EXTENSION IF NOT EXISTS "btree_gist" WITH SCHEMA "public";






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

create trigger on_auth_user_created
  after insert on auth.users
  for each row execute procedure public.handle_new_user();


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

create trigger on_auth_user_updated
  after update on auth.users
  for each row execute procedure public.handle_updated_user();

SET default_tablespace = '';

SET default_table_access_method = "heap";


CREATE TABLE IF NOT EXISTS "jobrunr"."jobrunr_backgroundjobservers" (
    "id" character(36) NOT NULL,
    "workerpoolsize" integer NOT NULL,
    "pollintervalinseconds" integer NOT NULL,
    "firstheartbeat" timestamp(6) without time zone NOT NULL,
    "lastheartbeat" timestamp(6) without time zone NOT NULL,
    "running" integer NOT NULL,
    "systemtotalmemory" bigint NOT NULL,
    "systemfreememory" bigint NOT NULL,
    "systemcpuload" numeric(3,2) NOT NULL,
    "processmaxmemory" bigint NOT NULL,
    "processfreememory" bigint NOT NULL,
    "processallocatedmemory" bigint NOT NULL,
    "processcpuload" numeric(3,2) NOT NULL,
    "deletesucceededjobsafter" character varying(32),
    "permanentlydeletejobsafter" character varying(32),
    "name" character varying(128)
);


ALTER TABLE "jobrunr"."jobrunr_backgroundjobservers" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "jobrunr"."jobrunr_jobs" (
    "id" character(36) NOT NULL,
    "version" integer NOT NULL,
    "jobasjson" "text" NOT NULL,
    "jobsignature" character varying(512) NOT NULL,
    "state" character varying(36) NOT NULL,
    "createdat" timestamp without time zone NOT NULL,
    "updatedat" timestamp without time zone NOT NULL,
    "scheduledat" timestamp without time zone,
    "recurringjobid" character varying(128)
);


ALTER TABLE "jobrunr"."jobrunr_jobs" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "jobrunr"."jobrunr_metadata" (
    "id" character varying(156) NOT NULL,
    "name" character varying(92) NOT NULL,
    "owner" character varying(64) NOT NULL,
    "value" "text" NOT NULL,
    "createdat" timestamp without time zone NOT NULL,
    "updatedat" timestamp without time zone NOT NULL
);


ALTER TABLE "jobrunr"."jobrunr_metadata" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "jobrunr"."jobrunr_recurring_jobs" (
    "id" character(128) NOT NULL,
    "version" integer NOT NULL,
    "jobasjson" "text" NOT NULL,
    "createdat" bigint DEFAULT '0'::bigint NOT NULL
);


ALTER TABLE "jobrunr"."jobrunr_recurring_jobs" OWNER TO "postgres";


CREATE OR REPLACE VIEW "jobrunr"."jobrunr_jobs_stats" AS
 WITH "job_stat_results" AS (
         SELECT "jobrunr_jobs"."state",
            "count"(*) AS "count"
           FROM "jobrunr"."jobrunr_jobs"
          GROUP BY "jobrunr_jobs"."state"
        )
 SELECT COALESCE(( SELECT "sum"("job_stat_results"."count") AS "sum"
           FROM "job_stat_results"), (0)::numeric) AS "total",
    COALESCE(( SELECT "sum"("job_stat_results"."count") AS "sum"
           FROM "job_stat_results"
          WHERE (("job_stat_results"."state")::"text" = 'AWAITING'::"text")), (0)::numeric) AS "awaiting",
    COALESCE(( SELECT "sum"("job_stat_results"."count") AS "sum"
           FROM "job_stat_results"
          WHERE (("job_stat_results"."state")::"text" = 'SCHEDULED'::"text")), (0)::numeric) AS "scheduled",
    COALESCE(( SELECT "sum"("job_stat_results"."count") AS "sum"
           FROM "job_stat_results"
          WHERE (("job_stat_results"."state")::"text" = 'ENQUEUED'::"text")), (0)::numeric) AS "enqueued",
    COALESCE(( SELECT "sum"("job_stat_results"."count") AS "sum"
           FROM "job_stat_results"
          WHERE (("job_stat_results"."state")::"text" = 'PROCESSING'::"text")), (0)::numeric) AS "processing",
    COALESCE(( SELECT "sum"("job_stat_results"."count") AS "sum"
           FROM "job_stat_results"
          WHERE (("job_stat_results"."state")::"text" = 'PROCESSED'::"text")), (0)::numeric) AS "processed",
    COALESCE(( SELECT "sum"("job_stat_results"."count") AS "sum"
           FROM "job_stat_results"
          WHERE (("job_stat_results"."state")::"text" = 'FAILED'::"text")), (0)::numeric) AS "failed",
    COALESCE(( SELECT "sum"("job_stat_results"."count") AS "sum"
           FROM "job_stat_results"
          WHERE (("job_stat_results"."state")::"text" = 'SUCCEEDED'::"text")), (0)::numeric) AS "succeeded",
    COALESCE(( SELECT (("jm"."value")::character(10))::numeric(10,0) AS "value"
           FROM "jobrunr"."jobrunr_metadata" "jm"
          WHERE (("jm"."id")::"text" = 'succeeded-jobs-counter-cluster'::"text")), (0)::numeric) AS "alltimesucceeded",
    COALESCE(( SELECT "sum"("job_stat_results"."count") AS "sum"
           FROM "job_stat_results"
          WHERE (("job_stat_results"."state")::"text" = 'DELETED'::"text")), (0)::numeric) AS "deleted",
    ( SELECT "count"(*) AS "count"
           FROM "jobrunr"."jobrunr_backgroundjobservers") AS "nbrofbackgroundjobservers",
    ( SELECT "count"(*) AS "count"
           FROM "jobrunr"."jobrunr_recurring_jobs") AS "nbrofrecurringjobs";


ALTER TABLE "jobrunr"."jobrunr_jobs_stats" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "jobrunr"."jobrunr_migrations" (
    "id" character(36) NOT NULL,
    "script" character varying(64) NOT NULL,
    "installedon" character varying(29) NOT NULL
);


ALTER TABLE "jobrunr"."jobrunr_migrations" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."catalog_item_categories" (
    "type" "text" NOT NULL,
    "code" "text" NOT NULL,
    "name" "text" NOT NULL,
    "description" "text",
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "created_by" "text",
    "last_modified_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "last_modified_by" "text",
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL
);


ALTER TABLE "public"."catalog_item_categories" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."catalog_items" (
    "type" "text" NOT NULL,
    "code" "text" NOT NULL,
    "name" "text" NOT NULL,
    "unit_of_measure" "text" NOT NULL,
    "vat_code" "text" NOT NULL,
    "consignor_party_id" "uuid",
    "default_price" numeric(15,4),
    "status" "text" NOT NULL,
    "purchase_account_number" "text",
    "sales_account_number" "text",
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "created_by" "text",
    "last_modified_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "last_modified_by" "text",
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "category_id" "uuid"
);


ALTER TABLE "public"."catalog_items" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."companies" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "name" "text" NOT NULL,
    "chamber_of_commerce_id" "text",
    "street_name" "text" NOT NULL,
    "postal_code" "text" NOT NULL,
    "city" "text" NOT NULL,
    "vihb_id" "text",
    "building_number" "text",
    "country" "text",
    "processor_id" "text",
    "building_number_addition" "text",
    "deleted_at" timestamp with time zone,
    "email" "text",
    "is_customer" boolean DEFAULT false NOT NULL,
    "is_supplier" boolean DEFAULT false NOT NULL,
    "phone" "text",
    "deleted_by" "uuid",
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "created_by" "text",
    "last_modified_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "last_modified_by" "text",
    "code" "text"
);


ALTER TABLE "public"."companies" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."companies_sync" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "company_id" "uuid",
    "last_timestamp" bigint,
    "synced_from_source_at" timestamp with time zone,
    "sync_error_message" "text",
    "deleted_in_source" boolean NOT NULL,
    "created_at" timestamp with time zone NOT NULL,
    "updated_at" timestamp with time zone,
    "sync_status" "text" NOT NULL,
    "conflict_details" "jsonb",
    "deleted_in_exact_at" timestamp with time zone,
    "deleted_in_exact_by" "uuid",
    "deleted_locally_at" timestamp with time zone,
    "exact_guid" "uuid"
);


ALTER TABLE "public"."companies_sync" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."companies_sync_cursor" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "entity" "text" NOT NULL,
    "last_timestamp" bigint NOT NULL,
    "updated_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "cursor_type" "text" DEFAULT 'sync'::"text" NOT NULL
);


ALTER TABLE "public"."companies_sync_cursor" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."company_project_locations" (
    "building_number" "text" NOT NULL,
    "building_number_addition" "text",
    "city" "text" NOT NULL,
    "postal_code" "text" NOT NULL,
    "street_name" "text" NOT NULL,
    "company_id" "uuid" NOT NULL,
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "updated_at" timestamp with time zone,
    "id" "uuid" NOT NULL,
    "country" "text" NOT NULL
);


ALTER TABLE "public"."company_project_locations" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."company_roles" (
    "company_id" "uuid" NOT NULL,
    "roles" "text" NOT NULL
);


ALTER TABLE "public"."company_roles" OWNER TO "postgres";


CREATE SEQUENCE IF NOT EXISTS "public"."edge_function_outbox_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE "public"."edge_function_outbox_id_seq" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."edge_function_outbox" (
    "id" bigint DEFAULT "nextval"('"public"."edge_function_outbox_id_seq"'::"regclass") NOT NULL,
    "function_name" "text" NOT NULL,
    "http_method" "text" NOT NULL,
    "payload" "jsonb",
    "status" "text" NOT NULL,
    "attempts" integer DEFAULT 0 NOT NULL,
    "last_attempt_at" timestamp with time zone,
    "error_message" "text",
    "processed_at" timestamp with time zone,
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "aggregate_type" "text",
    "aggregate_id" "text"
);


ALTER TABLE "public"."edge_function_outbox" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."eural" (
    "code" "text" NOT NULL,
    "description" "text" NOT NULL,
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "created_by" "text",
    "last_modified_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "last_modified_by" "text"
);


ALTER TABLE "public"."eural" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."exact_tokens" (
    "id" "uuid" NOT NULL,
    "access_token" "text" NOT NULL,
    "refresh_token" "text" NOT NULL,
    "token_type" "text" NOT NULL,
    "expires_at" timestamp with time zone NOT NULL,
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "updated_at" timestamp with time zone DEFAULT "now"() NOT NULL
);


ALTER TABLE "public"."exact_tokens" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."invoice_lines" (
    "line_number" integer NOT NULL,
    "line_date" "date" NOT NULL,
    "description" "text" NOT NULL,
    "order_reference" "text",
    "vat_code" "text" NOT NULL,
    "vat_percentage" numeric(5,2) NOT NULL,
    "gl_account_code" "text",
    "quantity" numeric(15,4) NOT NULL,
    "unit_price" numeric(15,4) NOT NULL,
    "total_excl_vat" numeric(15,2) NOT NULL,
    "unit_of_measure" "text" NOT NULL,
    "catalog_item_code" "text" NOT NULL,
    "catalog_item_name" "text" NOT NULL,
    "catalog_item_type" "text" NOT NULL,
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "invoice_id" "uuid" NOT NULL,
    "catalog_item_id" "uuid",
    CONSTRAINT "invoice_lines_catalog_item_type_check" CHECK (("catalog_item_type" = ANY (ARRAY['MATERIAL'::"text", 'PRODUCT'::"text", 'WASTE_STREAM'::"text"])))
);


ALTER TABLE "public"."invoice_lines" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."invoice_number_sequences" (
    "year" integer NOT NULL,
    "last_sequence" bigint DEFAULT 0 NOT NULL
);


ALTER TABLE "public"."invoice_number_sequences" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."invoices" (
    "invoice_number" "text",
    "invoice_type" "text" NOT NULL,
    "document_type" "text" NOT NULL,
    "status" "text" NOT NULL,
    "invoice_date" "date" NOT NULL,
    "customer_company_id" "uuid" NOT NULL,
    "customer_number" "text",
    "customer_name" "text" NOT NULL,
    "customer_street_name" "text" NOT NULL,
    "customer_building_number" "text",
    "customer_building_number_addition" "text",
    "customer_postal_code" "text" NOT NULL,
    "customer_city" "text" NOT NULL,
    "customer_country" "text",
    "customer_vat_number" "text",
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "created_by" "text",
    "last_modified_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "last_modified_by" "text",
    "finalized_at" timestamp with time zone,
    "finalized_by" "text",
    "pdf_url" "text",
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "original_invoice_id" "uuid",
    "source_weight_ticket_id" "uuid",
    "credited_invoice_number" "text"
);


ALTER TABLE "public"."invoices" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."lma_declaration_sessions" (
    "id" "uuid" NOT NULL,
    "type" "text" NOT NULL,
    "declaration_ids" "text"[] NOT NULL,
    "status" "text" NOT NULL,
    "errors" "text"[],
    "created_at" timestamp with time zone NOT NULL,
    "processed_at" timestamp with time zone
);


ALTER TABLE "public"."lma_declaration_sessions" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."lma_declarations" (
    "id" "text" NOT NULL,
    "waste_stream_number" "text" NOT NULL,
    "period" "text" NOT NULL,
    "transporters" "text"[] NOT NULL,
    "total_weight" bigint NOT NULL,
    "total_shipments" bigint NOT NULL,
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "amice_uuid" "uuid",
    "errors" "text"[],
    "status" "text" NOT NULL,
    "type" "text" NOT NULL,
    "weight_ticket_ids" "uuid"[]
);


ALTER TABLE "public"."lma_declarations" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."lma_import_errors" (
    "id" "uuid" NOT NULL,
    "import_batch_id" "uuid" NOT NULL,
    "row_number" integer NOT NULL,
    "waste_stream_number" "text",
    "error_code" "text" NOT NULL,
    "error_message" "text" NOT NULL,
    "raw_data" "jsonb",
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "resolved_at" timestamp with time zone,
    "resolved_by" "text"
);


ALTER TABLE "public"."lma_import_errors" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."material_price_sync_log" (
    "id" bigint NOT NULL,
    "material_id" "uuid",
    "external_product_id" integer,
    "action" "text" NOT NULL,
    "price_synced" numeric(19,4),
    "price_status_sent" integer,
    "status" "text" NOT NULL,
    "error_message" "text",
    "synced_at" timestamp with time zone DEFAULT "now"(),
    "synced_by" "text"
);


ALTER TABLE "public"."material_price_sync_log" OWNER TO "postgres";


CREATE SEQUENCE IF NOT EXISTS "public"."material_price_sync_log_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE "public"."material_price_sync_log_id_seq" OWNER TO "postgres";


ALTER SEQUENCE "public"."material_price_sync_log_id_seq" OWNED BY "public"."material_price_sync_log"."id";



CREATE TABLE IF NOT EXISTS "public"."material_pricing_app_sync" (
    "id" bigint NOT NULL,
    "material_id" "uuid" NOT NULL,
    "publish_to_pricing_app" boolean DEFAULT false NOT NULL,
    "external_pricing_app_id" integer,
    "external_pricing_app_synced_at" timestamp with time zone,
    "last_synced_price" numeric(19,4),
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "last_modified_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "created_by" "text",
    "last_modified_by" "text",
    "external_pricing_app_name" "text" NOT NULL
);


ALTER TABLE "public"."material_pricing_app_sync" OWNER TO "postgres";


CREATE SEQUENCE IF NOT EXISTS "public"."material_pricing_app_sync_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE "public"."material_pricing_app_sync_id_seq" OWNER TO "postgres";


ALTER SEQUENCE "public"."material_pricing_app_sync_id_seq" OWNED BY "public"."material_pricing_app_sync"."id";



CREATE TABLE IF NOT EXISTS "public"."monthly_waste_declaration_jobs" (
    "id" "uuid" NOT NULL,
    "job_type" "text" NOT NULL,
    "year_month" "text" NOT NULL,
    "status" "text" NOT NULL,
    "created_at" timestamp with time zone NOT NULL,
    "fulfilled_at" timestamp with time zone
);


ALTER TABLE "public"."monthly_waste_declaration_jobs" OWNER TO "postgres";


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
    "company_id" "uuid",
    "name" "text"
);


ALTER TABLE "public"."pickup_locations" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."processing_methods" (
    "code" "text" NOT NULL,
    "description" "text" NOT NULL,
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "created_by" "text",
    "last_modified_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "last_modified_by" "text"
);


ALTER TABLE "public"."processing_methods" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."profiles" (
    "id" "uuid" NOT NULL,
    "first_name" "text" NOT NULL,
    "last_name" "text" NOT NULL
);


ALTER TABLE "public"."profiles" OWNER TO "postgres";


CREATE SEQUENCE IF NOT EXISTS "public"."receival_declaration_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE "public"."receival_declaration_id_seq" OWNER TO "postgres";


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


CREATE TABLE IF NOT EXISTS "public"."transport_goods" (
    "id" bigint NOT NULL,
    "transport_id" "uuid" NOT NULL,
    "net_net_weight" numeric NOT NULL,
    "quantity" integer NOT NULL,
    "unit" "text",
    "waste_stream_number" "text" NOT NULL
);


ALTER TABLE "public"."transport_goods" OWNER TO "postgres";


ALTER TABLE "public"."transport_goods" ALTER COLUMN "id" ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME "public"."transport_goods_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);



CREATE SEQUENCE IF NOT EXISTS "public"."transport_seq_25"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE "public"."transport_seq_25" OWNER TO "postgres";


CREATE SEQUENCE IF NOT EXISTS "public"."transport_seq_26"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE "public"."transport_seq_26" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."transports" (
    "delivery_date_time" timestamp(6) without time zone,
    "pickup_date_time" timestamp(6) without time zone,
    "carrier_party_id" "uuid",
    "consignor_party_id" "uuid",
    "driver_id" "uuid",
    "id" "uuid" NOT NULL,
    "delivery_location_id" character varying(255),
    "pickup_location_id" character varying(255),
    "transport_type" "text",
    "truck_id" character varying(255),
    "display_number" "text" NOT NULL,
    "container_id" "text",
    "note" "text",
    "container_operation" "text",
    "sequence_number" smallint NOT NULL,
    "transport_hours" numeric(3,1) DEFAULT NULL::numeric,
    "consignor_classification" smallint,
    "driver_note" "text",
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "created_by" "text",
    "last_modified_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "last_modified_by" "text",
    "weight_ticket_id" "uuid",
    CONSTRAINT "transports_transport_hours_check" CHECK ((("transport_hours" IS NULL) OR ("transport_hours" >= (0)::numeric)))
);


ALTER TABLE "public"."transports" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."trucks" (
    "license_plate" character varying NOT NULL,
    "brand" character varying NOT NULL,
    "carrier_party_id" "uuid",
    "description" "text",
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "created_by" "text",
    "last_modified_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "last_modified_by" "text"
);


ALTER TABLE "public"."trucks" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."user_roles" (
    "user_id" "uuid" NOT NULL,
    "role" "public"."app_roles" NOT NULL,
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL
);


ALTER TABLE "public"."user_roles" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."vat_rates" (
    "vat_code" "text" NOT NULL,
    "percentage" numeric NOT NULL,
    "valid_from" timestamp with time zone NOT NULL,
    "valid_to" timestamp with time zone,
    "country_code" "text" NOT NULL,
    "description" "text" NOT NULL,
    "validity" "tstzrange" GENERATED ALWAYS AS ("tstzrange"("valid_from", COALESCE("valid_to", 'infinity'::timestamp with time zone), '[)'::"text")) STORED,
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "created_by" "text",
    "last_modified_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "last_modified_by" "text",
    CONSTRAINT "vat_rates_valid_range_chk" CHECK ((("valid_to" IS NULL) OR ("valid_to" > "valid_from")))
);


ALTER TABLE "public"."vat_rates" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."waste_containers" (
    "id" "text" NOT NULL,
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "updated_at" timestamp with time zone DEFAULT "now"(),
    "notes" "text",
    "location_id" "text",
    "created_by" "text",
    "last_modified_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "last_modified_by" "text"
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
    "status" "text" DEFAULT 'DRAFT'::"text" NOT NULL,
    "consignor_classification" smallint,
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "created_by" "text",
    "last_modified_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "last_modified_by" "text",
    "catalog_item_id" "uuid"
);


ALTER TABLE "public"."waste_streams" OWNER TO "postgres";


CREATE SEQUENCE IF NOT EXISTS "public"."weight_ticket_declaration_snapshots_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE "public"."weight_ticket_declaration_snapshots_id_seq" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."weight_ticket_lines" (
    "waste_stream_number" "text" DEFAULT ''::"text",
    "weight_value" numeric NOT NULL,
    "weight_unit" "text" NOT NULL,
    "declared_weight" numeric,
    "last_declared_at" timestamp with time zone,
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "catalog_item_id" "uuid" NOT NULL,
    "weight_ticket_id" "uuid" NOT NULL
);


ALTER TABLE "public"."weight_ticket_lines" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."weight_ticket_product_lines" (
    "id" "uuid" NOT NULL,
    "weight_ticket_id" "uuid" NOT NULL,
    "catalog_item_id" "uuid" NOT NULL,
    "quantity" numeric NOT NULL,
    "unit" "text" NOT NULL
);


ALTER TABLE "public"."weight_ticket_product_lines" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."weight_tickets" (
    "number" bigint NOT NULL,
    "consignor_party_id" "uuid" NOT NULL,
    "carrier_party_id" "uuid",
    "truck_license_plate" character varying,
    "reclamation" "text",
    "note" "text",
    "status" "text" NOT NULL,
    "weighted_at" timestamp with time zone,
    "cancellation_reason" "text",
    "delivery_location_id" "text",
    "direction" "text" DEFAULT ''::"text" NOT NULL,
    "pickup_location_id" "text",
    "tarra_weight_unit" "text",
    "tarra_weight_value" numeric,
    "second_weighing_unit" "text",
    "second_weighing_value" numeric,
    "pdf_url" "text",
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "created_by" "text",
    "last_modified_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "last_modified_by" "text",
    "linked_invoice_id" "uuid",
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL
);

ALTER TABLE "public"."weight_tickets" OWNER TO "postgres";


ALTER TABLE "public"."weight_tickets" ALTER COLUMN "number" ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME "public"."weight_tickets_id_seq"
    START WITH 50000
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);



ALTER TABLE ONLY "public"."material_price_sync_log" ALTER COLUMN "id" SET DEFAULT "nextval"('"public"."material_price_sync_log_id_seq"'::"regclass");



ALTER TABLE ONLY "public"."material_pricing_app_sync" ALTER COLUMN "id" SET DEFAULT "nextval"('"public"."material_pricing_app_sync_id_seq"'::"regclass");



ALTER TABLE ONLY "jobrunr"."jobrunr_backgroundjobservers"
    ADD CONSTRAINT "jobrunr_backgroundjobservers_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "jobrunr"."jobrunr_jobs"
    ADD CONSTRAINT "jobrunr_jobs_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "jobrunr"."jobrunr_metadata"
    ADD CONSTRAINT "jobrunr_metadata_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "jobrunr"."jobrunr_migrations"
    ADD CONSTRAINT "jobrunr_migrations_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "jobrunr"."jobrunr_recurring_jobs"
    ADD CONSTRAINT "jobrunr_recurring_jobs_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."catalog_item_categories"
    ADD CONSTRAINT "catalog_item_categories_code_key" UNIQUE ("code");



ALTER TABLE ONLY "public"."catalog_item_categories"
    ADD CONSTRAINT "catalog_item_categories_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."catalog_items"
    ADD CONSTRAINT "catalog_items_code_key" UNIQUE ("code");



ALTER TABLE ONLY "public"."catalog_items"
    ADD CONSTRAINT "catalog_items_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."companies"
    ADD CONSTRAINT "companies_processor_id_key" UNIQUE ("processor_id");



ALTER TABLE ONLY "public"."companies_sync"
    ADD CONSTRAINT "companies_sync_company_id_unique" UNIQUE ("company_id");



ALTER TABLE ONLY "public"."companies_sync_cursor"
    ADD CONSTRAINT "companies_sync_cursor_entity_type_unique" UNIQUE ("entity", "cursor_type");



ALTER TABLE ONLY "public"."companies_sync_cursor"
    ADD CONSTRAINT "companies_sync_cursor_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."companies"
    ADD CONSTRAINT "companies_vihb_number_key" UNIQUE ("vihb_id");



ALTER TABLE ONLY "public"."companies"
    ADD CONSTRAINT "company_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."company_project_locations"
    ADD CONSTRAINT "company_project_locations_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."edge_function_outbox"
    ADD CONSTRAINT "edge_function_outbox_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."eural"
    ADD CONSTRAINT "eural_pkey" PRIMARY KEY ("code");



ALTER TABLE ONLY "public"."companies_sync"
    ADD CONSTRAINT "exact_sync_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."exact_tokens"
    ADD CONSTRAINT "exact_tokens_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."invoice_lines"
    ADD CONSTRAINT "invoice_lines_invoice_id_line_number_key" UNIQUE ("invoice_id", "line_number");



ALTER TABLE ONLY "public"."invoice_lines"
    ADD CONSTRAINT "invoice_lines_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."invoice_number_sequences"
    ADD CONSTRAINT "invoice_number_sequences_pkey" PRIMARY KEY ("year");



ALTER TABLE ONLY "public"."invoices"
    ADD CONSTRAINT "invoices_invoice_number_key" UNIQUE ("invoice_number");



ALTER TABLE ONLY "public"."invoices"
    ADD CONSTRAINT "invoices_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."lma_declaration_sessions"
    ADD CONSTRAINT "lma_declaration_sessions_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."lma_declarations"
    ADD CONSTRAINT "lma_declarations_amice_uuid_key" UNIQUE ("amice_uuid");



ALTER TABLE ONLY "public"."lma_declarations"
    ADD CONSTRAINT "lma_declarations_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."lma_import_errors"
    ADD CONSTRAINT "lma_import_errors_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."material_price_sync_log"
    ADD CONSTRAINT "material_price_sync_log_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."material_pricing_app_sync"
    ADD CONSTRAINT "material_pricing_app_sync_material_id_key" UNIQUE ("material_id");



ALTER TABLE ONLY "public"."material_pricing_app_sync"
    ADD CONSTRAINT "material_pricing_app_sync_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."monthly_waste_declaration_jobs"
    ADD CONSTRAINT "monthly_waste_declaration_jobs_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."pickup_locations"
    ADD CONSTRAINT "pickup_locations_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."processing_methods"
    ADD CONSTRAINT "processing_methods_pkey" PRIMARY KEY ("code");



ALTER TABLE ONLY "public"."profiles"
    ADD CONSTRAINT "profiles_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."signatures"
    ADD CONSTRAINT "signatures_pkey" PRIMARY KEY ("transport_id");



ALTER TABLE ONLY "public"."transport_goods"
    ADD CONSTRAINT "transport_goods_pkey" PRIMARY KEY ("id");



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



ALTER TABLE ONLY "public"."vat_rates"
    ADD CONSTRAINT "vat_rates_no_overlap" EXCLUDE USING "gist" ("vat_code" WITH =, "country_code" WITH =, "validity" WITH &&);



ALTER TABLE ONLY "public"."vat_rates"
    ADD CONSTRAINT "vat_rates_pkey" PRIMARY KEY ("vat_code", "country_code", "valid_from");



ALTER TABLE ONLY "public"."waste_containers"
    ADD CONSTRAINT "waste_containers_id_key" UNIQUE ("id");



ALTER TABLE ONLY "public"."waste_containers"
    ADD CONSTRAINT "waste_containers_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."waste_streams"
    ADD CONSTRAINT "waste_streams_pkey" PRIMARY KEY ("number");



ALTER TABLE ONLY "public"."weight_ticket_lines"
    ADD CONSTRAINT "weight_ticket_lines_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."weight_ticket_product_lines"
    ADD CONSTRAINT "weight_ticket_product_lines_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."weight_tickets"
    ADD CONSTRAINT "weight_tickets_number_unique" UNIQUE ("number");



ALTER TABLE ONLY "public"."weight_tickets"
    ADD CONSTRAINT "weight_tickets_pkey" PRIMARY KEY ("id");



CREATE INDEX "jobrunr_bgjobsrvrs_fsthb_idx" ON "jobrunr"."jobrunr_backgroundjobservers" USING "btree" ("firstheartbeat");



CREATE INDEX "jobrunr_bgjobsrvrs_lsthb_idx" ON "jobrunr"."jobrunr_backgroundjobservers" USING "btree" ("lastheartbeat");



CREATE INDEX "jobrunr_job_created_at_idx" ON "jobrunr"."jobrunr_jobs" USING "btree" ("createdat");



CREATE INDEX "jobrunr_job_rci_idx" ON "jobrunr"."jobrunr_jobs" USING "btree" ("recurringjobid");



CREATE INDEX "jobrunr_job_scheduled_at_idx" ON "jobrunr"."jobrunr_jobs" USING "btree" ("scheduledat");



CREATE INDEX "jobrunr_job_signature_idx" ON "jobrunr"."jobrunr_jobs" USING "btree" ("jobsignature");



CREATE INDEX "jobrunr_jobs_state_updated_idx" ON "jobrunr"."jobrunr_jobs" USING "btree" ("state", "updatedat");



CREATE INDEX "jobrunr_recurring_job_created_at_idx" ON "jobrunr"."jobrunr_recurring_jobs" USING "btree" ("createdat");



CREATE INDEX "jobrunr_state_idx" ON "jobrunr"."jobrunr_jobs" USING "btree" ("state");



CREATE INDEX "idx_catalog_item_categories_type" ON "public"."catalog_item_categories" USING "btree" ("type");



CREATE INDEX "idx_catalog_items_category_id" ON "public"."catalog_items" USING "btree" ("category_id");



CREATE INDEX "idx_catalog_items_consignor" ON "public"."catalog_items" USING "btree" ("consignor_party_id");



CREATE INDEX "idx_catalog_items_status" ON "public"."catalog_items" USING "btree" ("status");



CREATE INDEX "idx_catalog_items_type" ON "public"."catalog_items" USING "btree" ("type");



CREATE INDEX "idx_companies_chamber_of_commerce_id" ON "public"."companies" USING "btree" ("chamber_of_commerce_id") WHERE (("chamber_of_commerce_id" IS NOT NULL) AND ("deleted_at" IS NULL));



CREATE INDEX "idx_companies_code" ON "public"."companies" USING "btree" ("code");



CREATE INDEX "idx_company_roles_company_id" ON "public"."company_roles" USING "btree" ("company_id");



CREATE INDEX "idx_edge_function_outbox_created_at" ON "public"."edge_function_outbox" USING "btree" ("created_at");



CREATE INDEX "idx_edge_function_outbox_status" ON "public"."edge_function_outbox" USING "btree" ("status");



CREATE INDEX "idx_invoice_lines_catalog_item_id" ON "public"."invoice_lines" USING "btree" ("catalog_item_id");



CREATE INDEX "idx_invoice_lines_invoice_id" ON "public"."invoice_lines" USING "btree" ("invoice_id");



CREATE INDEX "idx_invoices_customer" ON "public"."invoices" USING "btree" ("customer_company_id");



CREATE INDEX "idx_invoices_date" ON "public"."invoices" USING "btree" ("invoice_date");



CREATE INDEX "idx_invoices_status" ON "public"."invoices" USING "btree" ("status");



CREATE INDEX "idx_material_price_sync_log_material_id" ON "public"."material_price_sync_log" USING "btree" ("material_id");



CREATE INDEX "idx_material_price_sync_log_synced_at" ON "public"."material_price_sync_log" USING "btree" ("synced_at");



CREATE INDEX "idx_material_pricing_app_sync_material_id" ON "public"."material_pricing_app_sync" USING "btree" ("material_id");



CREATE INDEX "idx_material_pricing_app_sync_publish" ON "public"."material_pricing_app_sync" USING "btree" ("publish_to_pricing_app") WHERE ("publish_to_pricing_app" = true);



CREATE INDEX "idx_weight_ticket_lines_catalog_item_id" ON "public"."weight_ticket_lines" USING "btree" ("catalog_item_id");



CREATE INDEX "idx_weight_ticket_lines_declaration_state" ON "public"."weight_ticket_lines" USING "btree" ("declared_weight", "last_declared_at") WHERE ("declared_weight" IS NOT NULL);



CREATE INDEX "weight_ticket_product_lines_weight_ticket_id_idx" ON "public"."weight_ticket_product_lines" USING "btree" ("weight_ticket_id");



ALTER TABLE ONLY "public"."catalog_items"
    ADD CONSTRAINT "catalog_items_category_id_fkey" FOREIGN KEY ("category_id") REFERENCES "public"."catalog_item_categories"("id");



ALTER TABLE ONLY "public"."catalog_items"
    ADD CONSTRAINT "catalog_items_consignor_party_id_fkey" FOREIGN KEY ("consignor_party_id") REFERENCES "public"."companies"("id");



ALTER TABLE ONLY "public"."company_project_locations"
    ADD CONSTRAINT "company_project_locations_company_id_fkey" FOREIGN KEY ("company_id") REFERENCES "public"."companies"("id");



ALTER TABLE ONLY "public"."companies_sync"
    ADD CONSTRAINT "exact_sync_company_id_fkey" FOREIGN KEY ("company_id") REFERENCES "public"."companies"("id") ON DELETE SET NULL;



ALTER TABLE ONLY "public"."transports"
    ADD CONSTRAINT "fk2lgi3xage1g6g2daa75jhnw1c" FOREIGN KEY ("truck_id") REFERENCES "public"."trucks"("license_plate");



ALTER TABLE ONLY "public"."transports"
    ADD CONSTRAINT "fk9vtcum4wow21lygabe1baemab" FOREIGN KEY ("carrier_party_id") REFERENCES "public"."companies"("id");



ALTER TABLE ONLY "public"."company_roles"
    ADD CONSTRAINT "fk_company_roles_company_id" FOREIGN KEY ("company_id") REFERENCES "public"."companies"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."weight_ticket_lines"
    ADD CONSTRAINT "fk_weight_ticket_lines_weight_ticket" FOREIGN KEY ("weight_ticket_id") REFERENCES "public"."weight_tickets"("id");



ALTER TABLE ONLY "public"."transports"
    ADD CONSTRAINT "fkpfpjw6nu1nix3orr0vc2c2na6" FOREIGN KEY ("consignor_party_id") REFERENCES "public"."companies"("id");



ALTER TABLE ONLY "public"."invoice_lines"
    ADD CONSTRAINT "invoice_lines_catalog_item_id_fkey" FOREIGN KEY ("catalog_item_id") REFERENCES "public"."catalog_items"("id");



ALTER TABLE ONLY "public"."invoice_lines"
    ADD CONSTRAINT "invoice_lines_invoice_id_fkey" FOREIGN KEY ("invoice_id") REFERENCES "public"."invoices"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."invoices"
    ADD CONSTRAINT "invoices_customer_company_id_fkey" FOREIGN KEY ("customer_company_id") REFERENCES "public"."companies"("id");



ALTER TABLE ONLY "public"."invoices"
    ADD CONSTRAINT "invoices_original_invoice_id_fkey" FOREIGN KEY ("original_invoice_id") REFERENCES "public"."invoices"("id");



ALTER TABLE ONLY "public"."invoices"
    ADD CONSTRAINT "invoices_source_weight_ticket_id_fkey" FOREIGN KEY ("source_weight_ticket_id") REFERENCES "public"."weight_tickets"("id");



ALTER TABLE ONLY "public"."lma_declarations"
    ADD CONSTRAINT "lma_declarations_waste_stream_number_fkey" FOREIGN KEY ("waste_stream_number") REFERENCES "public"."waste_streams"("number");



ALTER TABLE ONLY "public"."material_price_sync_log"
    ADD CONSTRAINT "material_price_sync_log_material_id_fkey" FOREIGN KEY ("material_id") REFERENCES "public"."catalog_items"("id");



ALTER TABLE ONLY "public"."material_pricing_app_sync"
    ADD CONSTRAINT "material_pricing_app_sync_material_id_fkey" FOREIGN KEY ("material_id") REFERENCES "public"."catalog_items"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."profiles"
    ADD CONSTRAINT "profiles_id_fkey" FOREIGN KEY ("id") REFERENCES "auth"."users"("id") ON UPDATE CASCADE ON DELETE CASCADE;



ALTER TABLE ONLY "public"."signatures"
    ADD CONSTRAINT "signatures_transport_id_fkey" FOREIGN KEY ("transport_id") REFERENCES "public"."transports"("id");



ALTER TABLE ONLY "public"."transport_goods"
    ADD CONSTRAINT "transport_goods_transport_id_fkey" FOREIGN KEY ("transport_id") REFERENCES "public"."transports"("id");



ALTER TABLE ONLY "public"."transport_goods"
    ADD CONSTRAINT "transport_goods_waste_stream_number_fkey" FOREIGN KEY ("waste_stream_number") REFERENCES "public"."waste_streams"("number");



ALTER TABLE ONLY "public"."transports"
    ADD CONSTRAINT "transports_container_id_fkey" FOREIGN KEY ("container_id") REFERENCES "public"."waste_containers"("id");



ALTER TABLE ONLY "public"."transports"
    ADD CONSTRAINT "transports_delivery_location_id_fkey" FOREIGN KEY ("delivery_location_id") REFERENCES "public"."pickup_locations"("id");



ALTER TABLE ONLY "public"."transports"
    ADD CONSTRAINT "transports_driver_id_fkey" FOREIGN KEY ("driver_id") REFERENCES "public"."profiles"("id");



ALTER TABLE ONLY "public"."transports"
    ADD CONSTRAINT "transports_pickup_location_id_fkey" FOREIGN KEY ("pickup_location_id") REFERENCES "public"."pickup_locations"("id");



ALTER TABLE ONLY "public"."transports"
    ADD CONSTRAINT "transports_weight_ticket_id_fkey" FOREIGN KEY ("weight_ticket_id") REFERENCES "public"."weight_tickets"("id");



ALTER TABLE ONLY "public"."trucks"
    ADD CONSTRAINT "trucks_carrier_party_id_fkey" FOREIGN KEY ("carrier_party_id") REFERENCES "public"."companies"("id");



ALTER TABLE ONLY "public"."user_roles"
    ADD CONSTRAINT "user_roles_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "auth"."users"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."waste_containers"
    ADD CONSTRAINT "waste_containers_location_id_fkey" FOREIGN KEY ("location_id") REFERENCES "public"."pickup_locations"("id");



ALTER TABLE ONLY "public"."waste_streams"
    ADD CONSTRAINT "waste_streams_broker_party_id_fkey" FOREIGN KEY ("broker_party_id") REFERENCES "public"."companies"("id");



ALTER TABLE ONLY "public"."waste_streams"
    ADD CONSTRAINT "waste_streams_catalog_item_id_fkey" FOREIGN KEY ("catalog_item_id") REFERENCES "public"."catalog_items"("id");



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
    ADD CONSTRAINT "weight_ticket_lines_catalog_item_id_fkey" FOREIGN KEY ("catalog_item_id") REFERENCES "public"."catalog_items"("id");



ALTER TABLE ONLY "public"."weight_ticket_lines"
    ADD CONSTRAINT "weight_ticket_lines_waste_stream_number_fkey" FOREIGN KEY ("waste_stream_number") REFERENCES "public"."waste_streams"("number");



ALTER TABLE ONLY "public"."weight_ticket_product_lines"
    ADD CONSTRAINT "weight_ticket_product_lines_catalog_item_id_fkey" FOREIGN KEY ("catalog_item_id") REFERENCES "public"."catalog_items"("id");



ALTER TABLE ONLY "public"."weight_ticket_product_lines"
    ADD CONSTRAINT "weight_ticket_product_lines_weight_ticket_id_fkey" FOREIGN KEY ("weight_ticket_id") REFERENCES "public"."weight_tickets"("id") ON UPDATE CASCADE ON DELETE CASCADE;



ALTER TABLE ONLY "public"."weight_tickets"
    ADD CONSTRAINT "weight_tickets_carrier_party_id_fkey" FOREIGN KEY ("carrier_party_id") REFERENCES "public"."companies"("id");



ALTER TABLE ONLY "public"."weight_tickets"
    ADD CONSTRAINT "weight_tickets_consignor_party_id_fkey" FOREIGN KEY ("consignor_party_id") REFERENCES "public"."companies"("id");



ALTER TABLE ONLY "public"."weight_tickets"
    ADD CONSTRAINT "weight_tickets_delivery_location_id_fkey" FOREIGN KEY ("delivery_location_id") REFERENCES "public"."pickup_locations"("id");



ALTER TABLE ONLY "public"."weight_tickets"
    ADD CONSTRAINT "weight_tickets_linked_invoice_id_fkey" FOREIGN KEY ("linked_invoice_id") REFERENCES "public"."invoices"("id");



ALTER TABLE ONLY "public"."weight_tickets"
    ADD CONSTRAINT "weight_tickets_pickup_location_id_fkey" FOREIGN KEY ("pickup_location_id") REFERENCES "public"."pickup_locations"("id");



ALTER TABLE ONLY "public"."weight_tickets"
    ADD CONSTRAINT "weight_tickets_truck_license_plate_fkey" FOREIGN KEY ("truck_license_plate") REFERENCES "public"."trucks"("license_plate");



CREATE POLICY "Allow auth admin to read user roles" ON "public"."user_roles" FOR SELECT TO "supabase_auth_admin" USING (true);



CREATE POLICY "Enable all access for authenticated users only" ON "public"."companies" USING (true) WITH CHECK (true);



CREATE POLICY "Enable all access for authenticated users only" ON "public"."trucks" TO "authenticated" USING (true) WITH CHECK (true);



ALTER TABLE "public"."catalog_item_categories" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."catalog_items" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."companies" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."companies_sync" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."companies_sync_cursor" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."company_project_locations" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."company_roles" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."edge_function_outbox" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."eural" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."exact_tokens" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."invoice_lines" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."invoice_number_sequences" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."invoices" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."lma_declaration_sessions" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."lma_declarations" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."lma_import_errors" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."material_price_sync_log" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."material_pricing_app_sync" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."monthly_waste_declaration_jobs" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."pickup_locations" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."processing_methods" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."profiles" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."signatures" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."transport_goods" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."transports" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."trucks" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."user_roles" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."vat_rates" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."waste_containers" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."waste_streams" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."weight_ticket_lines" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."weight_ticket_product_lines" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."weight_tickets" ENABLE ROW LEVEL SECURITY;




ALTER PUBLICATION "supabase_realtime" OWNER TO "postgres";


GRANT USAGE ON SCHEMA "public" TO "postgres";
GRANT USAGE ON SCHEMA "public" TO "anon";
GRANT USAGE ON SCHEMA "public" TO "authenticated";
GRANT USAGE ON SCHEMA "public" TO "service_role";
GRANT USAGE ON SCHEMA "public" TO "supabase_auth_admin";



GRANT ALL ON FUNCTION "public"."gbtreekey16_in"("cstring") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbtreekey16_in"("cstring") TO "anon";
GRANT ALL ON FUNCTION "public"."gbtreekey16_in"("cstring") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbtreekey16_in"("cstring") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbtreekey16_out"("public"."gbtreekey16") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbtreekey16_out"("public"."gbtreekey16") TO "anon";
GRANT ALL ON FUNCTION "public"."gbtreekey16_out"("public"."gbtreekey16") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbtreekey16_out"("public"."gbtreekey16") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbtreekey2_in"("cstring") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbtreekey2_in"("cstring") TO "anon";
GRANT ALL ON FUNCTION "public"."gbtreekey2_in"("cstring") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbtreekey2_in"("cstring") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbtreekey2_out"("public"."gbtreekey2") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbtreekey2_out"("public"."gbtreekey2") TO "anon";
GRANT ALL ON FUNCTION "public"."gbtreekey2_out"("public"."gbtreekey2") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbtreekey2_out"("public"."gbtreekey2") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbtreekey32_in"("cstring") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbtreekey32_in"("cstring") TO "anon";
GRANT ALL ON FUNCTION "public"."gbtreekey32_in"("cstring") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbtreekey32_in"("cstring") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbtreekey32_out"("public"."gbtreekey32") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbtreekey32_out"("public"."gbtreekey32") TO "anon";
GRANT ALL ON FUNCTION "public"."gbtreekey32_out"("public"."gbtreekey32") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbtreekey32_out"("public"."gbtreekey32") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbtreekey4_in"("cstring") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbtreekey4_in"("cstring") TO "anon";
GRANT ALL ON FUNCTION "public"."gbtreekey4_in"("cstring") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbtreekey4_in"("cstring") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbtreekey4_out"("public"."gbtreekey4") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbtreekey4_out"("public"."gbtreekey4") TO "anon";
GRANT ALL ON FUNCTION "public"."gbtreekey4_out"("public"."gbtreekey4") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbtreekey4_out"("public"."gbtreekey4") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbtreekey8_in"("cstring") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbtreekey8_in"("cstring") TO "anon";
GRANT ALL ON FUNCTION "public"."gbtreekey8_in"("cstring") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbtreekey8_in"("cstring") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbtreekey8_out"("public"."gbtreekey8") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbtreekey8_out"("public"."gbtreekey8") TO "anon";
GRANT ALL ON FUNCTION "public"."gbtreekey8_out"("public"."gbtreekey8") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbtreekey8_out"("public"."gbtreekey8") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbtreekey_var_in"("cstring") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbtreekey_var_in"("cstring") TO "anon";
GRANT ALL ON FUNCTION "public"."gbtreekey_var_in"("cstring") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbtreekey_var_in"("cstring") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbtreekey_var_out"("public"."gbtreekey_var") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbtreekey_var_out"("public"."gbtreekey_var") TO "anon";
GRANT ALL ON FUNCTION "public"."gbtreekey_var_out"("public"."gbtreekey_var") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbtreekey_var_out"("public"."gbtreekey_var") TO "service_role";


  create policy "Authenticated can list and upload bd9ltp_0"
  on "storage"."objects"
  as permissive
  for select
  to authenticated
using ((bucket_id = 'waybills'::text));



  create policy "Authenticated can list and upload bd9ltp_1"
  on "storage"."objects"
  as permissive
  for insert
  to authenticated
with check ((bucket_id = 'waybills'::text));



  create policy "Authenticated can list and upload bd9ltp_2"
  on "storage"."objects"
  as permissive
  for update
  to authenticated
using ((bucket_id = 'waybills'::text));



  create policy "Authenticated users can read invoices PDFs"
  on "storage"."objects"
  as permissive
  for select
  to authenticated
using ((bucket_id = 'invoices'::text));



  create policy "Authenticated users can read weight ticket PDFs"
  on "storage"."objects"
  as permissive
  for select
  to authenticated
using ((bucket_id = 'weight-tickets'::text));



  create policy "Service role can delete invoices PDFs"
  on "storage"."objects"
  as permissive
  for delete
  to service_role
using ((bucket_id = 'invoices'::text));



  create policy "Service role can delete weight ticket PDFs"
  on "storage"."objects"
  as permissive
  for delete
  to service_role
using ((bucket_id = 'weight-tickets'::text));



  create policy "Service role can insert invoices PDFs"
  on "storage"."objects"
  as permissive
  for insert
  to service_role
with check ((bucket_id = 'invoices'::text));



  create policy "Service role can insert weight ticket PDFs"
  on "storage"."objects"
  as permissive
  for insert
  to service_role
with check ((bucket_id = 'weight-tickets'::text));



  create policy "Service role can update invoices PDFs"
  on "storage"."objects"
  as permissive
  for update
  to service_role
using ((bucket_id = 'invoices'::text));



  create policy "Service role can update weight ticket PDFs"
  on "storage"."objects"
  as permissive
  for update
  to service_role
using ((bucket_id = 'weight-tickets'::text));


















































































































































































GRANT ALL ON FUNCTION "public"."cash_dist"("money", "money") TO "postgres";
GRANT ALL ON FUNCTION "public"."cash_dist"("money", "money") TO "anon";
GRANT ALL ON FUNCTION "public"."cash_dist"("money", "money") TO "authenticated";
GRANT ALL ON FUNCTION "public"."cash_dist"("money", "money") TO "service_role";



GRANT ALL ON FUNCTION "public"."custom_access_token_hook"("event" "jsonb") TO "anon";
GRANT ALL ON FUNCTION "public"."custom_access_token_hook"("event" "jsonb") TO "authenticated";
GRANT ALL ON FUNCTION "public"."custom_access_token_hook"("event" "jsonb") TO "service_role";
GRANT ALL ON FUNCTION "public"."custom_access_token_hook"("event" "jsonb") TO "supabase_auth_admin";



GRANT ALL ON FUNCTION "public"."date_dist"("date", "date") TO "postgres";
GRANT ALL ON FUNCTION "public"."date_dist"("date", "date") TO "anon";
GRANT ALL ON FUNCTION "public"."date_dist"("date", "date") TO "authenticated";
GRANT ALL ON FUNCTION "public"."date_dist"("date", "date") TO "service_role";



GRANT ALL ON FUNCTION "public"."float4_dist"(real, real) TO "postgres";
GRANT ALL ON FUNCTION "public"."float4_dist"(real, real) TO "anon";
GRANT ALL ON FUNCTION "public"."float4_dist"(real, real) TO "authenticated";
GRANT ALL ON FUNCTION "public"."float4_dist"(real, real) TO "service_role";



GRANT ALL ON FUNCTION "public"."float8_dist"(double precision, double precision) TO "postgres";
GRANT ALL ON FUNCTION "public"."float8_dist"(double precision, double precision) TO "anon";
GRANT ALL ON FUNCTION "public"."float8_dist"(double precision, double precision) TO "authenticated";
GRANT ALL ON FUNCTION "public"."float8_dist"(double precision, double precision) TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_bit_compress"("internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_bit_compress"("internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_bit_compress"("internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_bit_compress"("internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_bit_consistent"("internal", bit, smallint, "oid", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_bit_consistent"("internal", bit, smallint, "oid", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_bit_consistent"("internal", bit, smallint, "oid", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_bit_consistent"("internal", bit, smallint, "oid", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_bit_penalty"("internal", "internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_bit_penalty"("internal", "internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_bit_penalty"("internal", "internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_bit_penalty"("internal", "internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_bit_picksplit"("internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_bit_picksplit"("internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_bit_picksplit"("internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_bit_picksplit"("internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_bit_same"("public"."gbtreekey_var", "public"."gbtreekey_var", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_bit_same"("public"."gbtreekey_var", "public"."gbtreekey_var", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_bit_same"("public"."gbtreekey_var", "public"."gbtreekey_var", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_bit_same"("public"."gbtreekey_var", "public"."gbtreekey_var", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_bit_union"("internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_bit_union"("internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_bit_union"("internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_bit_union"("internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_bool_compress"("internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_bool_compress"("internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_bool_compress"("internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_bool_compress"("internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_bool_consistent"("internal", boolean, smallint, "oid", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_bool_consistent"("internal", boolean, smallint, "oid", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_bool_consistent"("internal", boolean, smallint, "oid", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_bool_consistent"("internal", boolean, smallint, "oid", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_bool_fetch"("internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_bool_fetch"("internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_bool_fetch"("internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_bool_fetch"("internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_bool_penalty"("internal", "internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_bool_penalty"("internal", "internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_bool_penalty"("internal", "internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_bool_penalty"("internal", "internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_bool_picksplit"("internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_bool_picksplit"("internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_bool_picksplit"("internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_bool_picksplit"("internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_bool_same"("public"."gbtreekey2", "public"."gbtreekey2", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_bool_same"("public"."gbtreekey2", "public"."gbtreekey2", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_bool_same"("public"."gbtreekey2", "public"."gbtreekey2", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_bool_same"("public"."gbtreekey2", "public"."gbtreekey2", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_bool_union"("internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_bool_union"("internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_bool_union"("internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_bool_union"("internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_bpchar_compress"("internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_bpchar_compress"("internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_bpchar_compress"("internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_bpchar_compress"("internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_bpchar_consistent"("internal", character, smallint, "oid", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_bpchar_consistent"("internal", character, smallint, "oid", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_bpchar_consistent"("internal", character, smallint, "oid", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_bpchar_consistent"("internal", character, smallint, "oid", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_bytea_compress"("internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_bytea_compress"("internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_bytea_compress"("internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_bytea_compress"("internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_bytea_consistent"("internal", "bytea", smallint, "oid", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_bytea_consistent"("internal", "bytea", smallint, "oid", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_bytea_consistent"("internal", "bytea", smallint, "oid", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_bytea_consistent"("internal", "bytea", smallint, "oid", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_bytea_penalty"("internal", "internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_bytea_penalty"("internal", "internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_bytea_penalty"("internal", "internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_bytea_penalty"("internal", "internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_bytea_picksplit"("internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_bytea_picksplit"("internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_bytea_picksplit"("internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_bytea_picksplit"("internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_bytea_same"("public"."gbtreekey_var", "public"."gbtreekey_var", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_bytea_same"("public"."gbtreekey_var", "public"."gbtreekey_var", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_bytea_same"("public"."gbtreekey_var", "public"."gbtreekey_var", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_bytea_same"("public"."gbtreekey_var", "public"."gbtreekey_var", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_bytea_union"("internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_bytea_union"("internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_bytea_union"("internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_bytea_union"("internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_cash_compress"("internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_cash_compress"("internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_cash_compress"("internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_cash_compress"("internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_cash_consistent"("internal", "money", smallint, "oid", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_cash_consistent"("internal", "money", smallint, "oid", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_cash_consistent"("internal", "money", smallint, "oid", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_cash_consistent"("internal", "money", smallint, "oid", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_cash_distance"("internal", "money", smallint, "oid", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_cash_distance"("internal", "money", smallint, "oid", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_cash_distance"("internal", "money", smallint, "oid", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_cash_distance"("internal", "money", smallint, "oid", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_cash_fetch"("internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_cash_fetch"("internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_cash_fetch"("internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_cash_fetch"("internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_cash_penalty"("internal", "internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_cash_penalty"("internal", "internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_cash_penalty"("internal", "internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_cash_penalty"("internal", "internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_cash_picksplit"("internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_cash_picksplit"("internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_cash_picksplit"("internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_cash_picksplit"("internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_cash_same"("public"."gbtreekey16", "public"."gbtreekey16", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_cash_same"("public"."gbtreekey16", "public"."gbtreekey16", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_cash_same"("public"."gbtreekey16", "public"."gbtreekey16", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_cash_same"("public"."gbtreekey16", "public"."gbtreekey16", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_cash_union"("internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_cash_union"("internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_cash_union"("internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_cash_union"("internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_date_compress"("internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_date_compress"("internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_date_compress"("internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_date_compress"("internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_date_consistent"("internal", "date", smallint, "oid", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_date_consistent"("internal", "date", smallint, "oid", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_date_consistent"("internal", "date", smallint, "oid", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_date_consistent"("internal", "date", smallint, "oid", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_date_distance"("internal", "date", smallint, "oid", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_date_distance"("internal", "date", smallint, "oid", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_date_distance"("internal", "date", smallint, "oid", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_date_distance"("internal", "date", smallint, "oid", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_date_fetch"("internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_date_fetch"("internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_date_fetch"("internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_date_fetch"("internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_date_penalty"("internal", "internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_date_penalty"("internal", "internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_date_penalty"("internal", "internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_date_penalty"("internal", "internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_date_picksplit"("internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_date_picksplit"("internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_date_picksplit"("internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_date_picksplit"("internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_date_same"("public"."gbtreekey8", "public"."gbtreekey8", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_date_same"("public"."gbtreekey8", "public"."gbtreekey8", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_date_same"("public"."gbtreekey8", "public"."gbtreekey8", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_date_same"("public"."gbtreekey8", "public"."gbtreekey8", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_date_union"("internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_date_union"("internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_date_union"("internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_date_union"("internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_decompress"("internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_decompress"("internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_decompress"("internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_decompress"("internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_enum_compress"("internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_enum_compress"("internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_enum_compress"("internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_enum_compress"("internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_enum_consistent"("internal", "anyenum", smallint, "oid", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_enum_consistent"("internal", "anyenum", smallint, "oid", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_enum_consistent"("internal", "anyenum", smallint, "oid", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_enum_consistent"("internal", "anyenum", smallint, "oid", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_enum_fetch"("internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_enum_fetch"("internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_enum_fetch"("internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_enum_fetch"("internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_enum_penalty"("internal", "internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_enum_penalty"("internal", "internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_enum_penalty"("internal", "internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_enum_penalty"("internal", "internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_enum_picksplit"("internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_enum_picksplit"("internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_enum_picksplit"("internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_enum_picksplit"("internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_enum_same"("public"."gbtreekey8", "public"."gbtreekey8", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_enum_same"("public"."gbtreekey8", "public"."gbtreekey8", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_enum_same"("public"."gbtreekey8", "public"."gbtreekey8", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_enum_same"("public"."gbtreekey8", "public"."gbtreekey8", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_enum_union"("internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_enum_union"("internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_enum_union"("internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_enum_union"("internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_float4_compress"("internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_float4_compress"("internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_float4_compress"("internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_float4_compress"("internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_float4_consistent"("internal", real, smallint, "oid", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_float4_consistent"("internal", real, smallint, "oid", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_float4_consistent"("internal", real, smallint, "oid", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_float4_consistent"("internal", real, smallint, "oid", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_float4_distance"("internal", real, smallint, "oid", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_float4_distance"("internal", real, smallint, "oid", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_float4_distance"("internal", real, smallint, "oid", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_float4_distance"("internal", real, smallint, "oid", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_float4_fetch"("internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_float4_fetch"("internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_float4_fetch"("internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_float4_fetch"("internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_float4_penalty"("internal", "internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_float4_penalty"("internal", "internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_float4_penalty"("internal", "internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_float4_penalty"("internal", "internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_float4_picksplit"("internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_float4_picksplit"("internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_float4_picksplit"("internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_float4_picksplit"("internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_float4_same"("public"."gbtreekey8", "public"."gbtreekey8", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_float4_same"("public"."gbtreekey8", "public"."gbtreekey8", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_float4_same"("public"."gbtreekey8", "public"."gbtreekey8", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_float4_same"("public"."gbtreekey8", "public"."gbtreekey8", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_float4_union"("internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_float4_union"("internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_float4_union"("internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_float4_union"("internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_float8_compress"("internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_float8_compress"("internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_float8_compress"("internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_float8_compress"("internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_float8_consistent"("internal", double precision, smallint, "oid", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_float8_consistent"("internal", double precision, smallint, "oid", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_float8_consistent"("internal", double precision, smallint, "oid", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_float8_consistent"("internal", double precision, smallint, "oid", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_float8_distance"("internal", double precision, smallint, "oid", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_float8_distance"("internal", double precision, smallint, "oid", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_float8_distance"("internal", double precision, smallint, "oid", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_float8_distance"("internal", double precision, smallint, "oid", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_float8_fetch"("internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_float8_fetch"("internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_float8_fetch"("internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_float8_fetch"("internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_float8_penalty"("internal", "internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_float8_penalty"("internal", "internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_float8_penalty"("internal", "internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_float8_penalty"("internal", "internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_float8_picksplit"("internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_float8_picksplit"("internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_float8_picksplit"("internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_float8_picksplit"("internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_float8_same"("public"."gbtreekey16", "public"."gbtreekey16", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_float8_same"("public"."gbtreekey16", "public"."gbtreekey16", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_float8_same"("public"."gbtreekey16", "public"."gbtreekey16", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_float8_same"("public"."gbtreekey16", "public"."gbtreekey16", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_float8_union"("internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_float8_union"("internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_float8_union"("internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_float8_union"("internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_inet_compress"("internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_inet_compress"("internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_inet_compress"("internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_inet_compress"("internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_inet_consistent"("internal", "inet", smallint, "oid", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_inet_consistent"("internal", "inet", smallint, "oid", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_inet_consistent"("internal", "inet", smallint, "oid", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_inet_consistent"("internal", "inet", smallint, "oid", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_inet_penalty"("internal", "internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_inet_penalty"("internal", "internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_inet_penalty"("internal", "internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_inet_penalty"("internal", "internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_inet_picksplit"("internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_inet_picksplit"("internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_inet_picksplit"("internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_inet_picksplit"("internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_inet_same"("public"."gbtreekey16", "public"."gbtreekey16", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_inet_same"("public"."gbtreekey16", "public"."gbtreekey16", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_inet_same"("public"."gbtreekey16", "public"."gbtreekey16", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_inet_same"("public"."gbtreekey16", "public"."gbtreekey16", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_inet_union"("internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_inet_union"("internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_inet_union"("internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_inet_union"("internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_int2_compress"("internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_int2_compress"("internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_int2_compress"("internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_int2_compress"("internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_int2_consistent"("internal", smallint, smallint, "oid", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_int2_consistent"("internal", smallint, smallint, "oid", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_int2_consistent"("internal", smallint, smallint, "oid", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_int2_consistent"("internal", smallint, smallint, "oid", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_int2_distance"("internal", smallint, smallint, "oid", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_int2_distance"("internal", smallint, smallint, "oid", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_int2_distance"("internal", smallint, smallint, "oid", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_int2_distance"("internal", smallint, smallint, "oid", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_int2_fetch"("internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_int2_fetch"("internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_int2_fetch"("internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_int2_fetch"("internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_int2_penalty"("internal", "internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_int2_penalty"("internal", "internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_int2_penalty"("internal", "internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_int2_penalty"("internal", "internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_int2_picksplit"("internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_int2_picksplit"("internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_int2_picksplit"("internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_int2_picksplit"("internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_int2_same"("public"."gbtreekey4", "public"."gbtreekey4", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_int2_same"("public"."gbtreekey4", "public"."gbtreekey4", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_int2_same"("public"."gbtreekey4", "public"."gbtreekey4", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_int2_same"("public"."gbtreekey4", "public"."gbtreekey4", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_int2_union"("internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_int2_union"("internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_int2_union"("internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_int2_union"("internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_int4_compress"("internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_int4_compress"("internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_int4_compress"("internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_int4_compress"("internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_int4_consistent"("internal", integer, smallint, "oid", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_int4_consistent"("internal", integer, smallint, "oid", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_int4_consistent"("internal", integer, smallint, "oid", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_int4_consistent"("internal", integer, smallint, "oid", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_int4_distance"("internal", integer, smallint, "oid", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_int4_distance"("internal", integer, smallint, "oid", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_int4_distance"("internal", integer, smallint, "oid", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_int4_distance"("internal", integer, smallint, "oid", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_int4_fetch"("internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_int4_fetch"("internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_int4_fetch"("internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_int4_fetch"("internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_int4_penalty"("internal", "internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_int4_penalty"("internal", "internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_int4_penalty"("internal", "internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_int4_penalty"("internal", "internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_int4_picksplit"("internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_int4_picksplit"("internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_int4_picksplit"("internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_int4_picksplit"("internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_int4_same"("public"."gbtreekey8", "public"."gbtreekey8", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_int4_same"("public"."gbtreekey8", "public"."gbtreekey8", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_int4_same"("public"."gbtreekey8", "public"."gbtreekey8", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_int4_same"("public"."gbtreekey8", "public"."gbtreekey8", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_int4_union"("internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_int4_union"("internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_int4_union"("internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_int4_union"("internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_int8_compress"("internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_int8_compress"("internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_int8_compress"("internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_int8_compress"("internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_int8_consistent"("internal", bigint, smallint, "oid", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_int8_consistent"("internal", bigint, smallint, "oid", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_int8_consistent"("internal", bigint, smallint, "oid", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_int8_consistent"("internal", bigint, smallint, "oid", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_int8_distance"("internal", bigint, smallint, "oid", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_int8_distance"("internal", bigint, smallint, "oid", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_int8_distance"("internal", bigint, smallint, "oid", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_int8_distance"("internal", bigint, smallint, "oid", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_int8_fetch"("internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_int8_fetch"("internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_int8_fetch"("internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_int8_fetch"("internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_int8_penalty"("internal", "internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_int8_penalty"("internal", "internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_int8_penalty"("internal", "internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_int8_penalty"("internal", "internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_int8_picksplit"("internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_int8_picksplit"("internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_int8_picksplit"("internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_int8_picksplit"("internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_int8_same"("public"."gbtreekey16", "public"."gbtreekey16", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_int8_same"("public"."gbtreekey16", "public"."gbtreekey16", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_int8_same"("public"."gbtreekey16", "public"."gbtreekey16", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_int8_same"("public"."gbtreekey16", "public"."gbtreekey16", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_int8_union"("internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_int8_union"("internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_int8_union"("internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_int8_union"("internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_intv_compress"("internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_intv_compress"("internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_intv_compress"("internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_intv_compress"("internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_intv_consistent"("internal", interval, smallint, "oid", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_intv_consistent"("internal", interval, smallint, "oid", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_intv_consistent"("internal", interval, smallint, "oid", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_intv_consistent"("internal", interval, smallint, "oid", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_intv_decompress"("internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_intv_decompress"("internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_intv_decompress"("internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_intv_decompress"("internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_intv_distance"("internal", interval, smallint, "oid", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_intv_distance"("internal", interval, smallint, "oid", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_intv_distance"("internal", interval, smallint, "oid", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_intv_distance"("internal", interval, smallint, "oid", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_intv_fetch"("internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_intv_fetch"("internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_intv_fetch"("internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_intv_fetch"("internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_intv_penalty"("internal", "internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_intv_penalty"("internal", "internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_intv_penalty"("internal", "internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_intv_penalty"("internal", "internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_intv_picksplit"("internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_intv_picksplit"("internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_intv_picksplit"("internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_intv_picksplit"("internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_intv_same"("public"."gbtreekey32", "public"."gbtreekey32", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_intv_same"("public"."gbtreekey32", "public"."gbtreekey32", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_intv_same"("public"."gbtreekey32", "public"."gbtreekey32", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_intv_same"("public"."gbtreekey32", "public"."gbtreekey32", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_intv_union"("internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_intv_union"("internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_intv_union"("internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_intv_union"("internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_macad8_compress"("internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_macad8_compress"("internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_macad8_compress"("internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_macad8_compress"("internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_macad8_consistent"("internal", "macaddr8", smallint, "oid", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_macad8_consistent"("internal", "macaddr8", smallint, "oid", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_macad8_consistent"("internal", "macaddr8", smallint, "oid", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_macad8_consistent"("internal", "macaddr8", smallint, "oid", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_macad8_fetch"("internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_macad8_fetch"("internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_macad8_fetch"("internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_macad8_fetch"("internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_macad8_penalty"("internal", "internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_macad8_penalty"("internal", "internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_macad8_penalty"("internal", "internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_macad8_penalty"("internal", "internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_macad8_picksplit"("internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_macad8_picksplit"("internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_macad8_picksplit"("internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_macad8_picksplit"("internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_macad8_same"("public"."gbtreekey16", "public"."gbtreekey16", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_macad8_same"("public"."gbtreekey16", "public"."gbtreekey16", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_macad8_same"("public"."gbtreekey16", "public"."gbtreekey16", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_macad8_same"("public"."gbtreekey16", "public"."gbtreekey16", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_macad8_union"("internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_macad8_union"("internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_macad8_union"("internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_macad8_union"("internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_macad_compress"("internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_macad_compress"("internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_macad_compress"("internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_macad_compress"("internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_macad_consistent"("internal", "macaddr", smallint, "oid", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_macad_consistent"("internal", "macaddr", smallint, "oid", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_macad_consistent"("internal", "macaddr", smallint, "oid", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_macad_consistent"("internal", "macaddr", smallint, "oid", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_macad_fetch"("internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_macad_fetch"("internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_macad_fetch"("internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_macad_fetch"("internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_macad_penalty"("internal", "internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_macad_penalty"("internal", "internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_macad_penalty"("internal", "internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_macad_penalty"("internal", "internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_macad_picksplit"("internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_macad_picksplit"("internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_macad_picksplit"("internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_macad_picksplit"("internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_macad_same"("public"."gbtreekey16", "public"."gbtreekey16", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_macad_same"("public"."gbtreekey16", "public"."gbtreekey16", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_macad_same"("public"."gbtreekey16", "public"."gbtreekey16", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_macad_same"("public"."gbtreekey16", "public"."gbtreekey16", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_macad_union"("internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_macad_union"("internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_macad_union"("internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_macad_union"("internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_numeric_compress"("internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_numeric_compress"("internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_numeric_compress"("internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_numeric_compress"("internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_numeric_consistent"("internal", numeric, smallint, "oid", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_numeric_consistent"("internal", numeric, smallint, "oid", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_numeric_consistent"("internal", numeric, smallint, "oid", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_numeric_consistent"("internal", numeric, smallint, "oid", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_numeric_penalty"("internal", "internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_numeric_penalty"("internal", "internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_numeric_penalty"("internal", "internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_numeric_penalty"("internal", "internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_numeric_picksplit"("internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_numeric_picksplit"("internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_numeric_picksplit"("internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_numeric_picksplit"("internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_numeric_same"("public"."gbtreekey_var", "public"."gbtreekey_var", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_numeric_same"("public"."gbtreekey_var", "public"."gbtreekey_var", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_numeric_same"("public"."gbtreekey_var", "public"."gbtreekey_var", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_numeric_same"("public"."gbtreekey_var", "public"."gbtreekey_var", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_numeric_union"("internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_numeric_union"("internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_numeric_union"("internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_numeric_union"("internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_oid_compress"("internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_oid_compress"("internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_oid_compress"("internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_oid_compress"("internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_oid_consistent"("internal", "oid", smallint, "oid", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_oid_consistent"("internal", "oid", smallint, "oid", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_oid_consistent"("internal", "oid", smallint, "oid", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_oid_consistent"("internal", "oid", smallint, "oid", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_oid_distance"("internal", "oid", smallint, "oid", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_oid_distance"("internal", "oid", smallint, "oid", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_oid_distance"("internal", "oid", smallint, "oid", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_oid_distance"("internal", "oid", smallint, "oid", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_oid_fetch"("internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_oid_fetch"("internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_oid_fetch"("internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_oid_fetch"("internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_oid_penalty"("internal", "internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_oid_penalty"("internal", "internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_oid_penalty"("internal", "internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_oid_penalty"("internal", "internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_oid_picksplit"("internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_oid_picksplit"("internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_oid_picksplit"("internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_oid_picksplit"("internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_oid_same"("public"."gbtreekey8", "public"."gbtreekey8", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_oid_same"("public"."gbtreekey8", "public"."gbtreekey8", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_oid_same"("public"."gbtreekey8", "public"."gbtreekey8", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_oid_same"("public"."gbtreekey8", "public"."gbtreekey8", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_oid_union"("internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_oid_union"("internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_oid_union"("internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_oid_union"("internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_text_compress"("internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_text_compress"("internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_text_compress"("internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_text_compress"("internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_text_consistent"("internal", "text", smallint, "oid", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_text_consistent"("internal", "text", smallint, "oid", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_text_consistent"("internal", "text", smallint, "oid", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_text_consistent"("internal", "text", smallint, "oid", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_text_penalty"("internal", "internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_text_penalty"("internal", "internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_text_penalty"("internal", "internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_text_penalty"("internal", "internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_text_picksplit"("internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_text_picksplit"("internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_text_picksplit"("internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_text_picksplit"("internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_text_same"("public"."gbtreekey_var", "public"."gbtreekey_var", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_text_same"("public"."gbtreekey_var", "public"."gbtreekey_var", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_text_same"("public"."gbtreekey_var", "public"."gbtreekey_var", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_text_same"("public"."gbtreekey_var", "public"."gbtreekey_var", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_text_union"("internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_text_union"("internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_text_union"("internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_text_union"("internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_time_compress"("internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_time_compress"("internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_time_compress"("internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_time_compress"("internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_time_consistent"("internal", time without time zone, smallint, "oid", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_time_consistent"("internal", time without time zone, smallint, "oid", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_time_consistent"("internal", time without time zone, smallint, "oid", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_time_consistent"("internal", time without time zone, smallint, "oid", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_time_distance"("internal", time without time zone, smallint, "oid", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_time_distance"("internal", time without time zone, smallint, "oid", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_time_distance"("internal", time without time zone, smallint, "oid", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_time_distance"("internal", time without time zone, smallint, "oid", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_time_fetch"("internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_time_fetch"("internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_time_fetch"("internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_time_fetch"("internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_time_penalty"("internal", "internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_time_penalty"("internal", "internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_time_penalty"("internal", "internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_time_penalty"("internal", "internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_time_picksplit"("internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_time_picksplit"("internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_time_picksplit"("internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_time_picksplit"("internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_time_same"("public"."gbtreekey16", "public"."gbtreekey16", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_time_same"("public"."gbtreekey16", "public"."gbtreekey16", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_time_same"("public"."gbtreekey16", "public"."gbtreekey16", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_time_same"("public"."gbtreekey16", "public"."gbtreekey16", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_time_union"("internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_time_union"("internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_time_union"("internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_time_union"("internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_timetz_compress"("internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_timetz_compress"("internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_timetz_compress"("internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_timetz_compress"("internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_timetz_consistent"("internal", time with time zone, smallint, "oid", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_timetz_consistent"("internal", time with time zone, smallint, "oid", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_timetz_consistent"("internal", time with time zone, smallint, "oid", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_timetz_consistent"("internal", time with time zone, smallint, "oid", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_ts_compress"("internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_ts_compress"("internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_ts_compress"("internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_ts_compress"("internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_ts_consistent"("internal", timestamp without time zone, smallint, "oid", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_ts_consistent"("internal", timestamp without time zone, smallint, "oid", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_ts_consistent"("internal", timestamp without time zone, smallint, "oid", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_ts_consistent"("internal", timestamp without time zone, smallint, "oid", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_ts_distance"("internal", timestamp without time zone, smallint, "oid", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_ts_distance"("internal", timestamp without time zone, smallint, "oid", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_ts_distance"("internal", timestamp without time zone, smallint, "oid", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_ts_distance"("internal", timestamp without time zone, smallint, "oid", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_ts_fetch"("internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_ts_fetch"("internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_ts_fetch"("internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_ts_fetch"("internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_ts_penalty"("internal", "internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_ts_penalty"("internal", "internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_ts_penalty"("internal", "internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_ts_penalty"("internal", "internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_ts_picksplit"("internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_ts_picksplit"("internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_ts_picksplit"("internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_ts_picksplit"("internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_ts_same"("public"."gbtreekey16", "public"."gbtreekey16", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_ts_same"("public"."gbtreekey16", "public"."gbtreekey16", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_ts_same"("public"."gbtreekey16", "public"."gbtreekey16", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_ts_same"("public"."gbtreekey16", "public"."gbtreekey16", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_ts_union"("internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_ts_union"("internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_ts_union"("internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_ts_union"("internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_tstz_compress"("internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_tstz_compress"("internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_tstz_compress"("internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_tstz_compress"("internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_tstz_consistent"("internal", timestamp with time zone, smallint, "oid", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_tstz_consistent"("internal", timestamp with time zone, smallint, "oid", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_tstz_consistent"("internal", timestamp with time zone, smallint, "oid", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_tstz_consistent"("internal", timestamp with time zone, smallint, "oid", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_tstz_distance"("internal", timestamp with time zone, smallint, "oid", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_tstz_distance"("internal", timestamp with time zone, smallint, "oid", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_tstz_distance"("internal", timestamp with time zone, smallint, "oid", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_tstz_distance"("internal", timestamp with time zone, smallint, "oid", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_uuid_compress"("internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_uuid_compress"("internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_uuid_compress"("internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_uuid_compress"("internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_uuid_consistent"("internal", "uuid", smallint, "oid", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_uuid_consistent"("internal", "uuid", smallint, "oid", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_uuid_consistent"("internal", "uuid", smallint, "oid", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_uuid_consistent"("internal", "uuid", smallint, "oid", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_uuid_fetch"("internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_uuid_fetch"("internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_uuid_fetch"("internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_uuid_fetch"("internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_uuid_penalty"("internal", "internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_uuid_penalty"("internal", "internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_uuid_penalty"("internal", "internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_uuid_penalty"("internal", "internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_uuid_picksplit"("internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_uuid_picksplit"("internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_uuid_picksplit"("internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_uuid_picksplit"("internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_uuid_same"("public"."gbtreekey32", "public"."gbtreekey32", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_uuid_same"("public"."gbtreekey32", "public"."gbtreekey32", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_uuid_same"("public"."gbtreekey32", "public"."gbtreekey32", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_uuid_same"("public"."gbtreekey32", "public"."gbtreekey32", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_uuid_union"("internal", "internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_uuid_union"("internal", "internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_uuid_union"("internal", "internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_uuid_union"("internal", "internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_var_decompress"("internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_var_decompress"("internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_var_decompress"("internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_var_decompress"("internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."gbt_var_fetch"("internal") TO "postgres";
GRANT ALL ON FUNCTION "public"."gbt_var_fetch"("internal") TO "anon";
GRANT ALL ON FUNCTION "public"."gbt_var_fetch"("internal") TO "authenticated";
GRANT ALL ON FUNCTION "public"."gbt_var_fetch"("internal") TO "service_role";



GRANT ALL ON FUNCTION "public"."handle_new_user"() TO "anon";
GRANT ALL ON FUNCTION "public"."handle_new_user"() TO "authenticated";
GRANT ALL ON FUNCTION "public"."handle_new_user"() TO "service_role";



GRANT ALL ON FUNCTION "public"."handle_updated_user"() TO "anon";
GRANT ALL ON FUNCTION "public"."handle_updated_user"() TO "authenticated";
GRANT ALL ON FUNCTION "public"."handle_updated_user"() TO "service_role";



GRANT ALL ON FUNCTION "public"."int2_dist"(smallint, smallint) TO "postgres";
GRANT ALL ON FUNCTION "public"."int2_dist"(smallint, smallint) TO "anon";
GRANT ALL ON FUNCTION "public"."int2_dist"(smallint, smallint) TO "authenticated";
GRANT ALL ON FUNCTION "public"."int2_dist"(smallint, smallint) TO "service_role";



GRANT ALL ON FUNCTION "public"."int4_dist"(integer, integer) TO "postgres";
GRANT ALL ON FUNCTION "public"."int4_dist"(integer, integer) TO "anon";
GRANT ALL ON FUNCTION "public"."int4_dist"(integer, integer) TO "authenticated";
GRANT ALL ON FUNCTION "public"."int4_dist"(integer, integer) TO "service_role";



GRANT ALL ON FUNCTION "public"."int8_dist"(bigint, bigint) TO "postgres";
GRANT ALL ON FUNCTION "public"."int8_dist"(bigint, bigint) TO "anon";
GRANT ALL ON FUNCTION "public"."int8_dist"(bigint, bigint) TO "authenticated";
GRANT ALL ON FUNCTION "public"."int8_dist"(bigint, bigint) TO "service_role";



GRANT ALL ON FUNCTION "public"."interval_dist"(interval, interval) TO "postgres";
GRANT ALL ON FUNCTION "public"."interval_dist"(interval, interval) TO "anon";
GRANT ALL ON FUNCTION "public"."interval_dist"(interval, interval) TO "authenticated";
GRANT ALL ON FUNCTION "public"."interval_dist"(interval, interval) TO "service_role";



GRANT ALL ON FUNCTION "public"."oid_dist"("oid", "oid") TO "postgres";
GRANT ALL ON FUNCTION "public"."oid_dist"("oid", "oid") TO "anon";
GRANT ALL ON FUNCTION "public"."oid_dist"("oid", "oid") TO "authenticated";
GRANT ALL ON FUNCTION "public"."oid_dist"("oid", "oid") TO "service_role";



GRANT ALL ON FUNCTION "public"."time_dist"(time without time zone, time without time zone) TO "postgres";
GRANT ALL ON FUNCTION "public"."time_dist"(time without time zone, time without time zone) TO "anon";
GRANT ALL ON FUNCTION "public"."time_dist"(time without time zone, time without time zone) TO "authenticated";
GRANT ALL ON FUNCTION "public"."time_dist"(time without time zone, time without time zone) TO "service_role";



GRANT ALL ON FUNCTION "public"."ts_dist"(timestamp without time zone, timestamp without time zone) TO "postgres";
GRANT ALL ON FUNCTION "public"."ts_dist"(timestamp without time zone, timestamp without time zone) TO "anon";
GRANT ALL ON FUNCTION "public"."ts_dist"(timestamp without time zone, timestamp without time zone) TO "authenticated";
GRANT ALL ON FUNCTION "public"."ts_dist"(timestamp without time zone, timestamp without time zone) TO "service_role";



GRANT ALL ON FUNCTION "public"."tstz_dist"(timestamp with time zone, timestamp with time zone) TO "postgres";
GRANT ALL ON FUNCTION "public"."tstz_dist"(timestamp with time zone, timestamp with time zone) TO "anon";
GRANT ALL ON FUNCTION "public"."tstz_dist"(timestamp with time zone, timestamp with time zone) TO "authenticated";
GRANT ALL ON FUNCTION "public"."tstz_dist"(timestamp with time zone, timestamp with time zone) TO "service_role";


















GRANT ALL ON TABLE "public"."catalog_item_categories" TO "anon";
GRANT ALL ON TABLE "public"."catalog_item_categories" TO "authenticated";
GRANT ALL ON TABLE "public"."catalog_item_categories" TO "service_role";



GRANT ALL ON TABLE "public"."catalog_items" TO "anon";
GRANT ALL ON TABLE "public"."catalog_items" TO "authenticated";
GRANT ALL ON TABLE "public"."catalog_items" TO "service_role";



GRANT ALL ON TABLE "public"."companies" TO "anon";
GRANT ALL ON TABLE "public"."companies" TO "authenticated";
GRANT ALL ON TABLE "public"."companies" TO "service_role";



GRANT ALL ON TABLE "public"."companies_sync" TO "anon";
GRANT ALL ON TABLE "public"."companies_sync" TO "authenticated";
GRANT ALL ON TABLE "public"."companies_sync" TO "service_role";



GRANT ALL ON TABLE "public"."companies_sync_cursor" TO "anon";
GRANT ALL ON TABLE "public"."companies_sync_cursor" TO "authenticated";
GRANT ALL ON TABLE "public"."companies_sync_cursor" TO "service_role";



GRANT ALL ON TABLE "public"."company_project_locations" TO "anon";
GRANT ALL ON TABLE "public"."company_project_locations" TO "authenticated";
GRANT ALL ON TABLE "public"."company_project_locations" TO "service_role";



GRANT ALL ON TABLE "public"."company_roles" TO "anon";
GRANT ALL ON TABLE "public"."company_roles" TO "authenticated";
GRANT ALL ON TABLE "public"."company_roles" TO "service_role";



GRANT ALL ON SEQUENCE "public"."edge_function_outbox_id_seq" TO "anon";
GRANT ALL ON SEQUENCE "public"."edge_function_outbox_id_seq" TO "authenticated";
GRANT ALL ON SEQUENCE "public"."edge_function_outbox_id_seq" TO "service_role";



GRANT ALL ON TABLE "public"."edge_function_outbox" TO "anon";
GRANT ALL ON TABLE "public"."edge_function_outbox" TO "authenticated";
GRANT ALL ON TABLE "public"."edge_function_outbox" TO "service_role";



GRANT ALL ON TABLE "public"."eural" TO "anon";
GRANT ALL ON TABLE "public"."eural" TO "authenticated";
GRANT ALL ON TABLE "public"."eural" TO "service_role";



GRANT ALL ON TABLE "public"."exact_tokens" TO "anon";
GRANT ALL ON TABLE "public"."exact_tokens" TO "authenticated";
GRANT ALL ON TABLE "public"."exact_tokens" TO "service_role";



GRANT ALL ON TABLE "public"."invoice_lines" TO "anon";
GRANT ALL ON TABLE "public"."invoice_lines" TO "authenticated";
GRANT ALL ON TABLE "public"."invoice_lines" TO "service_role";



GRANT ALL ON TABLE "public"."invoice_number_sequences" TO "anon";
GRANT ALL ON TABLE "public"."invoice_number_sequences" TO "authenticated";
GRANT ALL ON TABLE "public"."invoice_number_sequences" TO "service_role";



GRANT ALL ON TABLE "public"."invoices" TO "anon";
GRANT ALL ON TABLE "public"."invoices" TO "authenticated";
GRANT ALL ON TABLE "public"."invoices" TO "service_role";



GRANT ALL ON TABLE "public"."lma_declaration_sessions" TO "anon";
GRANT ALL ON TABLE "public"."lma_declaration_sessions" TO "authenticated";
GRANT ALL ON TABLE "public"."lma_declaration_sessions" TO "service_role";



GRANT ALL ON TABLE "public"."lma_declarations" TO "anon";
GRANT ALL ON TABLE "public"."lma_declarations" TO "authenticated";
GRANT ALL ON TABLE "public"."lma_declarations" TO "service_role";



GRANT ALL ON TABLE "public"."lma_import_errors" TO "anon";
GRANT ALL ON TABLE "public"."lma_import_errors" TO "authenticated";
GRANT ALL ON TABLE "public"."lma_import_errors" TO "service_role";



GRANT ALL ON TABLE "public"."material_price_sync_log" TO "anon";
GRANT ALL ON TABLE "public"."material_price_sync_log" TO "authenticated";
GRANT ALL ON TABLE "public"."material_price_sync_log" TO "service_role";



GRANT ALL ON SEQUENCE "public"."material_price_sync_log_id_seq" TO "anon";
GRANT ALL ON SEQUENCE "public"."material_price_sync_log_id_seq" TO "authenticated";
GRANT ALL ON SEQUENCE "public"."material_price_sync_log_id_seq" TO "service_role";



GRANT ALL ON TABLE "public"."material_pricing_app_sync" TO "anon";
GRANT ALL ON TABLE "public"."material_pricing_app_sync" TO "authenticated";
GRANT ALL ON TABLE "public"."material_pricing_app_sync" TO "service_role";



GRANT ALL ON SEQUENCE "public"."material_pricing_app_sync_id_seq" TO "anon";
GRANT ALL ON SEQUENCE "public"."material_pricing_app_sync_id_seq" TO "authenticated";
GRANT ALL ON SEQUENCE "public"."material_pricing_app_sync_id_seq" TO "service_role";



GRANT ALL ON TABLE "public"."monthly_waste_declaration_jobs" TO "anon";
GRANT ALL ON TABLE "public"."monthly_waste_declaration_jobs" TO "authenticated";
GRANT ALL ON TABLE "public"."monthly_waste_declaration_jobs" TO "service_role";



GRANT ALL ON TABLE "public"."pickup_locations" TO "anon";
GRANT ALL ON TABLE "public"."pickup_locations" TO "authenticated";
GRANT ALL ON TABLE "public"."pickup_locations" TO "service_role";



GRANT ALL ON TABLE "public"."processing_methods" TO "anon";
GRANT ALL ON TABLE "public"."processing_methods" TO "authenticated";
GRANT ALL ON TABLE "public"."processing_methods" TO "service_role";



GRANT ALL ON TABLE "public"."profiles" TO "anon";
GRANT ALL ON TABLE "public"."profiles" TO "authenticated";
GRANT ALL ON TABLE "public"."profiles" TO "service_role";



GRANT ALL ON SEQUENCE "public"."receival_declaration_id_seq" TO "anon";
GRANT ALL ON SEQUENCE "public"."receival_declaration_id_seq" TO "authenticated";
GRANT ALL ON SEQUENCE "public"."receival_declaration_id_seq" TO "service_role";



GRANT ALL ON TABLE "public"."signatures" TO "anon";
GRANT ALL ON TABLE "public"."signatures" TO "authenticated";
GRANT ALL ON TABLE "public"."signatures" TO "service_role";



GRANT ALL ON TABLE "public"."transport_goods" TO "anon";
GRANT ALL ON TABLE "public"."transport_goods" TO "authenticated";
GRANT ALL ON TABLE "public"."transport_goods" TO "service_role";



GRANT ALL ON SEQUENCE "public"."transport_goods_id_seq" TO "anon";
GRANT ALL ON SEQUENCE "public"."transport_goods_id_seq" TO "authenticated";
GRANT ALL ON SEQUENCE "public"."transport_goods_id_seq" TO "service_role";



GRANT ALL ON SEQUENCE "public"."transport_seq_25" TO "anon";
GRANT ALL ON SEQUENCE "public"."transport_seq_25" TO "authenticated";
GRANT ALL ON SEQUENCE "public"."transport_seq_25" TO "service_role";



GRANT ALL ON SEQUENCE "public"."transport_seq_26" TO "anon";
GRANT ALL ON SEQUENCE "public"."transport_seq_26" TO "authenticated";
GRANT ALL ON SEQUENCE "public"."transport_seq_26" TO "service_role";



GRANT ALL ON TABLE "public"."transports" TO "anon";
GRANT ALL ON TABLE "public"."transports" TO "authenticated";
GRANT ALL ON TABLE "public"."transports" TO "service_role";



GRANT ALL ON TABLE "public"."trucks" TO "anon";
GRANT ALL ON TABLE "public"."trucks" TO "authenticated";
GRANT ALL ON TABLE "public"."trucks" TO "service_role";



GRANT ALL ON TABLE "public"."user_roles" TO "service_role";
GRANT ALL ON TABLE "public"."user_roles" TO "supabase_auth_admin";



GRANT ALL ON TABLE "public"."vat_rates" TO "anon";
GRANT ALL ON TABLE "public"."vat_rates" TO "authenticated";
GRANT ALL ON TABLE "public"."vat_rates" TO "service_role";



GRANT ALL ON TABLE "public"."waste_containers" TO "anon";
GRANT ALL ON TABLE "public"."waste_containers" TO "authenticated";
GRANT ALL ON TABLE "public"."waste_containers" TO "service_role";



GRANT ALL ON TABLE "public"."waste_streams" TO "anon";
GRANT ALL ON TABLE "public"."waste_streams" TO "authenticated";
GRANT ALL ON TABLE "public"."waste_streams" TO "service_role";



GRANT ALL ON SEQUENCE "public"."weight_ticket_declaration_snapshots_id_seq" TO "anon";
GRANT ALL ON SEQUENCE "public"."weight_ticket_declaration_snapshots_id_seq" TO "authenticated";
GRANT ALL ON SEQUENCE "public"."weight_ticket_declaration_snapshots_id_seq" TO "service_role";



GRANT ALL ON TABLE "public"."weight_ticket_lines" TO "anon";
GRANT ALL ON TABLE "public"."weight_ticket_lines" TO "authenticated";
GRANT ALL ON TABLE "public"."weight_ticket_lines" TO "service_role";



GRANT ALL ON TABLE "public"."weight_ticket_product_lines" TO "anon";
GRANT ALL ON TABLE "public"."weight_ticket_product_lines" TO "authenticated";
GRANT ALL ON TABLE "public"."weight_ticket_product_lines" TO "service_role";



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






























