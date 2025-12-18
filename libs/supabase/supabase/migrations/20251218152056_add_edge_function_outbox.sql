create sequence "public"."edge_function_outbox_id_seq";


  create table "public"."edge_function_outbox" (
    "id" bigint not null default nextval('public.edge_function_outbox_id_seq'::regclass),
    "function_name" text not null,
    "http_method" text not null,
    "payload" jsonb,
    "status" text not null,
    "attempts" integer not null default 0,
    "last_attempt_at" timestamp with time zone,
    "error_message" text,
    "processed_at" timestamp with time zone,
    "created_at" timestamp with time zone not null default now(),
    "aggregate_type" text,
    "aggregate_id" text
      );


alter table "public"."edge_function_outbox" enable row level security;

CREATE UNIQUE INDEX edge_function_outbox_pkey ON public.edge_function_outbox USING btree (id);

CREATE INDEX idx_edge_function_outbox_created_at ON public.edge_function_outbox USING btree (created_at);

CREATE INDEX idx_edge_function_outbox_status ON public.edge_function_outbox USING btree (status);

alter table "public"."edge_function_outbox" add constraint "edge_function_outbox_pkey" PRIMARY KEY using index "edge_function_outbox_pkey";

grant delete on table "public"."catalog_item_categories" to "postgres";

grant insert on table "public"."catalog_item_categories" to "postgres";

grant references on table "public"."catalog_item_categories" to "postgres";

grant select on table "public"."catalog_item_categories" to "postgres";

grant trigger on table "public"."catalog_item_categories" to "postgres";

grant truncate on table "public"."catalog_item_categories" to "postgres";

grant update on table "public"."catalog_item_categories" to "postgres";

grant delete on table "public"."catalog_items" to "postgres";

grant insert on table "public"."catalog_items" to "postgres";

grant references on table "public"."catalog_items" to "postgres";

grant select on table "public"."catalog_items" to "postgres";

grant trigger on table "public"."catalog_items" to "postgres";

grant truncate on table "public"."catalog_items" to "postgres";

grant update on table "public"."catalog_items" to "postgres";

grant delete on table "public"."edge_function_outbox" to "anon";

grant insert on table "public"."edge_function_outbox" to "anon";

grant references on table "public"."edge_function_outbox" to "anon";

grant select on table "public"."edge_function_outbox" to "anon";

grant trigger on table "public"."edge_function_outbox" to "anon";

grant truncate on table "public"."edge_function_outbox" to "anon";

grant update on table "public"."edge_function_outbox" to "anon";

grant delete on table "public"."edge_function_outbox" to "authenticated";

grant insert on table "public"."edge_function_outbox" to "authenticated";

grant references on table "public"."edge_function_outbox" to "authenticated";

grant select on table "public"."edge_function_outbox" to "authenticated";

grant trigger on table "public"."edge_function_outbox" to "authenticated";

grant truncate on table "public"."edge_function_outbox" to "authenticated";

grant update on table "public"."edge_function_outbox" to "authenticated";

grant delete on table "public"."edge_function_outbox" to "postgres";

grant insert on table "public"."edge_function_outbox" to "postgres";

grant references on table "public"."edge_function_outbox" to "postgres";

grant select on table "public"."edge_function_outbox" to "postgres";

grant trigger on table "public"."edge_function_outbox" to "postgres";

grant truncate on table "public"."edge_function_outbox" to "postgres";

grant update on table "public"."edge_function_outbox" to "postgres";

grant delete on table "public"."edge_function_outbox" to "service_role";

grant insert on table "public"."edge_function_outbox" to "service_role";

grant references on table "public"."edge_function_outbox" to "service_role";

grant select on table "public"."edge_function_outbox" to "service_role";

grant trigger on table "public"."edge_function_outbox" to "service_role";

grant truncate on table "public"."edge_function_outbox" to "service_role";

grant update on table "public"."edge_function_outbox" to "service_role";

grant delete on table "public"."invoice_lines" to "postgres";

grant insert on table "public"."invoice_lines" to "postgres";

grant references on table "public"."invoice_lines" to "postgres";

grant select on table "public"."invoice_lines" to "postgres";

grant trigger on table "public"."invoice_lines" to "postgres";

grant truncate on table "public"."invoice_lines" to "postgres";

grant update on table "public"."invoice_lines" to "postgres";

grant delete on table "public"."invoice_number_sequences" to "postgres";

grant insert on table "public"."invoice_number_sequences" to "postgres";

grant references on table "public"."invoice_number_sequences" to "postgres";

grant select on table "public"."invoice_number_sequences" to "postgres";

grant trigger on table "public"."invoice_number_sequences" to "postgres";

grant truncate on table "public"."invoice_number_sequences" to "postgres";

grant update on table "public"."invoice_number_sequences" to "postgres";

grant delete on table "public"."invoices" to "postgres";

grant insert on table "public"."invoices" to "postgres";

grant references on table "public"."invoices" to "postgres";

grant select on table "public"."invoices" to "postgres";

grant trigger on table "public"."invoices" to "postgres";

grant truncate on table "public"."invoices" to "postgres";

grant update on table "public"."invoices" to "postgres";


