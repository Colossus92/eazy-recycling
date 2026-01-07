create table "public"."weight_ticket_product_lines" (
  "id" uuid not null,
  "weight_ticket_id" uuid not null,
  "catalog_item_id" uuid not null,
  "quantity" numeric not null,
  "unit" text not null
    );

alter table "public"."weight_ticket_product_lines" enable row level security;

CREATE UNIQUE INDEX weight_ticket_product_lines_pkey ON public.weight_ticket_product_lines USING btree (id);

CREATE INDEX weight_ticket_product_lines_weight_ticket_id_idx ON public.weight_ticket_product_lines USING btree (weight_ticket_id);

alter table "public"."weight_ticket_product_lines" add constraint "weight_ticket_product_lines_pkey" PRIMARY KEY using index "weight_ticket_product_lines_pkey";

alter table "public"."weight_ticket_product_lines" add constraint "weight_ticket_product_lines_catalog_item_id_fkey" FOREIGN KEY (catalog_item_id) REFERENCES public.catalog_items(id) not valid;

alter table "public"."weight_ticket_product_lines" validate constraint "weight_ticket_product_lines_catalog_item_id_fkey";

alter table "public"."weight_ticket_product_lines" add constraint "weight_ticket_product_lines_weight_ticket_id_fkey" FOREIGN KEY (weight_ticket_id) REFERENCES public.weight_tickets(id) ON UPDATE CASCADE ON DELETE CASCADE not valid;

alter table "public"."weight_ticket_product_lines" validate constraint "weight_ticket_product_lines_weight_ticket_id_fkey";