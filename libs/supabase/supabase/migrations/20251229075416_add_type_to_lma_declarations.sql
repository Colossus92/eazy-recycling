alter table "public"."lma_declarations" add column "type" text;

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

grant delete on table "public"."edge_function_outbox" to "postgres";

grant insert on table "public"."edge_function_outbox" to "postgres";

grant references on table "public"."edge_function_outbox" to "postgres";

grant select on table "public"."edge_function_outbox" to "postgres";

grant trigger on table "public"."edge_function_outbox" to "postgres";

grant truncate on table "public"."edge_function_outbox" to "postgres";

grant update on table "public"."edge_function_outbox" to "postgres";

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


