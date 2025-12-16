create sequence "public"."invoice_lines_id_seq";

create sequence "public"."invoices_id_seq";


  create table "public"."invoice_lines" (
    "id" bigint not null default nextval('public.invoice_lines_id_seq'::regclass),
    "invoice_id" bigint not null,
    "line_number" integer not null,
    "line_date" date not null,
    "description" text not null,
    "order_reference" text,
    "vat_code" text not null,
    "vat_percentage" numeric(5,2) not null,
    "gl_account_code" text,
    "quantity" numeric(15,4) not null,
    "unit_price" numeric(15,4) not null,
    "total_excl_vat" numeric(15,2) not null,
    "unit_of_measure" text not null,
    "catalog_item_id" bigint,
    "catalog_item_code" text not null,
    "catalog_item_name" text not null,
    "catalog_item_type" text not null
      );


alter table "public"."invoice_lines" enable row level security;


  create table "public"."invoice_number_sequences" (
    "year" integer not null,
    "last_sequence" bigint not null default 0
      );


alter table "public"."invoice_number_sequences" enable row level security;


  create table "public"."invoices" (
    "id" bigint not null default nextval('public.invoices_id_seq'::regclass),
    "invoice_number" text,
    "invoice_type" text not null,
    "document_type" text not null,
    "status" text not null,
    "invoice_date" date not null,
    "customer_company_id" uuid not null,
    "customer_number" text,
    "customer_name" text not null,
    "customer_street_name" text not null,
    "customer_building_number" text,
    "customer_building_number_addition" text,
    "customer_postal_code" text not null,
    "customer_city" text not null,
    "customer_country" text,
    "customer_vat_number" text,
    "original_invoice_id" bigint,
    "created_at" timestamp with time zone not null default now(),
    "created_by" text,
    "last_modified_at" timestamp with time zone not null default now(),
    "last_modified_by" text,
    "finalized_at" timestamp with time zone,
    "finalized_by" text
      );


alter table "public"."invoices" enable row level security;

CREATE INDEX idx_invoice_lines_invoice ON public.invoice_lines USING btree (invoice_id);

CREATE INDEX idx_invoices_customer ON public.invoices USING btree (customer_company_id);

CREATE INDEX idx_invoices_date ON public.invoices USING btree (invoice_date);

CREATE INDEX idx_invoices_status ON public.invoices USING btree (status);

CREATE UNIQUE INDEX invoice_lines_invoice_id_line_number_key ON public.invoice_lines USING btree (invoice_id, line_number);

CREATE UNIQUE INDEX invoice_lines_pkey ON public.invoice_lines USING btree (id);

CREATE UNIQUE INDEX invoice_number_sequences_pkey ON public.invoice_number_sequences USING btree (year);

CREATE UNIQUE INDEX invoices_invoice_number_key ON public.invoices USING btree (invoice_number);

CREATE UNIQUE INDEX invoices_pkey ON public.invoices USING btree (id);

alter table "public"."invoice_lines" add constraint "invoice_lines_pkey" PRIMARY KEY using index "invoice_lines_pkey";

alter table "public"."invoice_number_sequences" add constraint "invoice_number_sequences_pkey" PRIMARY KEY using index "invoice_number_sequences_pkey";

alter table "public"."invoices" add constraint "invoices_pkey" PRIMARY KEY using index "invoices_pkey";

alter table "public"."invoice_lines" add constraint "invoice_lines_catalog_item_id_fkey" FOREIGN KEY (catalog_item_id) REFERENCES public.catalog_items(id) not valid;

alter table "public"."invoice_lines" validate constraint "invoice_lines_catalog_item_id_fkey";

alter table "public"."invoice_lines" add constraint "invoice_lines_catalog_item_type_check" CHECK ((catalog_item_type = ANY (ARRAY['MATERIAL'::text, 'PRODUCT'::text, 'WASTE_STREAM'::text]))) not valid;

alter table "public"."invoice_lines" validate constraint "invoice_lines_catalog_item_type_check";

alter table "public"."invoice_lines" add constraint "invoice_lines_invoice_id_fkey" FOREIGN KEY (invoice_id) REFERENCES public.invoices(id) ON DELETE CASCADE not valid;

alter table "public"."invoice_lines" validate constraint "invoice_lines_invoice_id_fkey";

alter table "public"."invoice_lines" add constraint "invoice_lines_invoice_id_line_number_key" UNIQUE using index "invoice_lines_invoice_id_line_number_key";

alter table "public"."invoices" add constraint "invoices_customer_company_id_fkey" FOREIGN KEY (customer_company_id) REFERENCES public.companies(id) not valid;

alter table "public"."invoices" validate constraint "invoices_customer_company_id_fkey";

alter table "public"."invoices" add constraint "invoices_invoice_number_key" UNIQUE using index "invoices_invoice_number_key";

alter table "public"."invoices" add constraint "invoices_original_invoice_id_fkey" FOREIGN KEY (original_invoice_id) REFERENCES public.invoices(id) not valid;

alter table "public"."invoices" validate constraint "invoices_original_invoice_id_fkey";

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

grant delete on table "public"."invoice_lines" to "anon";

grant insert on table "public"."invoice_lines" to "anon";

grant references on table "public"."invoice_lines" to "anon";

grant select on table "public"."invoice_lines" to "anon";

grant trigger on table "public"."invoice_lines" to "anon";

grant truncate on table "public"."invoice_lines" to "anon";

grant update on table "public"."invoice_lines" to "anon";

grant delete on table "public"."invoice_lines" to "authenticated";

grant insert on table "public"."invoice_lines" to "authenticated";

grant references on table "public"."invoice_lines" to "authenticated";

grant select on table "public"."invoice_lines" to "authenticated";

grant trigger on table "public"."invoice_lines" to "authenticated";

grant truncate on table "public"."invoice_lines" to "authenticated";

grant update on table "public"."invoice_lines" to "authenticated";

grant delete on table "public"."invoice_lines" to "postgres";

grant insert on table "public"."invoice_lines" to "postgres";

grant references on table "public"."invoice_lines" to "postgres";

grant select on table "public"."invoice_lines" to "postgres";

grant trigger on table "public"."invoice_lines" to "postgres";

grant truncate on table "public"."invoice_lines" to "postgres";

grant update on table "public"."invoice_lines" to "postgres";

grant delete on table "public"."invoice_lines" to "service_role";

grant insert on table "public"."invoice_lines" to "service_role";

grant references on table "public"."invoice_lines" to "service_role";

grant select on table "public"."invoice_lines" to "service_role";

grant trigger on table "public"."invoice_lines" to "service_role";

grant truncate on table "public"."invoice_lines" to "service_role";

grant update on table "public"."invoice_lines" to "service_role";

grant delete on table "public"."invoice_number_sequences" to "anon";

grant insert on table "public"."invoice_number_sequences" to "anon";

grant references on table "public"."invoice_number_sequences" to "anon";

grant select on table "public"."invoice_number_sequences" to "anon";

grant trigger on table "public"."invoice_number_sequences" to "anon";

grant truncate on table "public"."invoice_number_sequences" to "anon";

grant update on table "public"."invoice_number_sequences" to "anon";

grant delete on table "public"."invoice_number_sequences" to "authenticated";

grant insert on table "public"."invoice_number_sequences" to "authenticated";

grant references on table "public"."invoice_number_sequences" to "authenticated";

grant select on table "public"."invoice_number_sequences" to "authenticated";

grant trigger on table "public"."invoice_number_sequences" to "authenticated";

grant truncate on table "public"."invoice_number_sequences" to "authenticated";

grant update on table "public"."invoice_number_sequences" to "authenticated";

grant delete on table "public"."invoice_number_sequences" to "postgres";

grant insert on table "public"."invoice_number_sequences" to "postgres";

grant references on table "public"."invoice_number_sequences" to "postgres";

grant select on table "public"."invoice_number_sequences" to "postgres";

grant trigger on table "public"."invoice_number_sequences" to "postgres";

grant truncate on table "public"."invoice_number_sequences" to "postgres";

grant update on table "public"."invoice_number_sequences" to "postgres";

grant delete on table "public"."invoice_number_sequences" to "service_role";

grant insert on table "public"."invoice_number_sequences" to "service_role";

grant references on table "public"."invoice_number_sequences" to "service_role";

grant select on table "public"."invoice_number_sequences" to "service_role";

grant trigger on table "public"."invoice_number_sequences" to "service_role";

grant truncate on table "public"."invoice_number_sequences" to "service_role";

grant update on table "public"."invoice_number_sequences" to "service_role";

grant delete on table "public"."invoices" to "anon";

grant insert on table "public"."invoices" to "anon";

grant references on table "public"."invoices" to "anon";

grant select on table "public"."invoices" to "anon";

grant trigger on table "public"."invoices" to "anon";

grant truncate on table "public"."invoices" to "anon";

grant update on table "public"."invoices" to "anon";

grant delete on table "public"."invoices" to "authenticated";

grant insert on table "public"."invoices" to "authenticated";

grant references on table "public"."invoices" to "authenticated";

grant select on table "public"."invoices" to "authenticated";

grant trigger on table "public"."invoices" to "authenticated";

grant truncate on table "public"."invoices" to "authenticated";

grant update on table "public"."invoices" to "authenticated";

grant delete on table "public"."invoices" to "postgres";

grant insert on table "public"."invoices" to "postgres";

grant references on table "public"."invoices" to "postgres";

grant select on table "public"."invoices" to "postgres";

grant trigger on table "public"."invoices" to "postgres";

grant truncate on table "public"."invoices" to "postgres";

grant update on table "public"."invoices" to "postgres";

grant delete on table "public"."invoices" to "service_role";

grant insert on table "public"."invoices" to "service_role";

grant references on table "public"."invoices" to "service_role";

grant select on table "public"."invoices" to "service_role";

grant trigger on table "public"."invoices" to "service_role";

grant truncate on table "public"."invoices" to "service_role";

grant update on table "public"."invoices" to "service_role";

grant delete on table "public"."product_categories" to "postgres";

grant insert on table "public"."product_categories" to "postgres";

grant references on table "public"."product_categories" to "postgres";

grant select on table "public"."product_categories" to "postgres";

grant trigger on table "public"."product_categories" to "postgres";

grant truncate on table "public"."product_categories" to "postgres";

grant update on table "public"."product_categories" to "postgres";

grant delete on table "public"."products" to "postgres";

grant insert on table "public"."products" to "postgres";

grant references on table "public"."products" to "postgres";

grant select on table "public"."products" to "postgres";

grant trigger on table "public"."products" to "postgres";

grant truncate on table "public"."products" to "postgres";

grant update on table "public"."products" to "postgres";


