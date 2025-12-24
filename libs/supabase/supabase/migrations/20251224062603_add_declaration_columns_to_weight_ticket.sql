revoke delete on table "public"."weight_ticket_declaration_snapshots" from "anon";

revoke insert on table "public"."weight_ticket_declaration_snapshots" from "anon";

revoke references on table "public"."weight_ticket_declaration_snapshots" from "anon";

revoke select on table "public"."weight_ticket_declaration_snapshots" from "anon";

revoke trigger on table "public"."weight_ticket_declaration_snapshots" from "anon";

revoke truncate on table "public"."weight_ticket_declaration_snapshots" from "anon";

revoke update on table "public"."weight_ticket_declaration_snapshots" from "anon";

revoke delete on table "public"."weight_ticket_declaration_snapshots" from "authenticated";

revoke insert on table "public"."weight_ticket_declaration_snapshots" from "authenticated";

revoke references on table "public"."weight_ticket_declaration_snapshots" from "authenticated";

revoke select on table "public"."weight_ticket_declaration_snapshots" from "authenticated";

revoke trigger on table "public"."weight_ticket_declaration_snapshots" from "authenticated";

revoke truncate on table "public"."weight_ticket_declaration_snapshots" from "authenticated";

revoke update on table "public"."weight_ticket_declaration_snapshots" from "authenticated";

revoke delete on table "public"."weight_ticket_declaration_snapshots" from "service_role";

revoke insert on table "public"."weight_ticket_declaration_snapshots" from "service_role";

revoke references on table "public"."weight_ticket_declaration_snapshots" from "service_role";

revoke select on table "public"."weight_ticket_declaration_snapshots" from "service_role";

revoke trigger on table "public"."weight_ticket_declaration_snapshots" from "service_role";

revoke truncate on table "public"."weight_ticket_declaration_snapshots" from "service_role";

revoke update on table "public"."weight_ticket_declaration_snapshots" from "service_role";

alter table "public"."weight_ticket_declaration_snapshots" drop constraint "fk_declaration_snapshots_declaration";

alter table "public"."weight_ticket_declaration_snapshots" drop constraint "fk_declaration_snapshots_weight_ticket";

alter table "public"."weight_ticket_declaration_snapshots" drop constraint "weight_ticket_declaration_snapshots_pkey";

drop index if exists "public"."idx_declaration_snapshots_period";

drop index if exists "public"."idx_declaration_snapshots_waste_stream";

drop index if exists "public"."idx_declaration_snapshots_weight_ticket";

drop index if exists "public"."weight_ticket_declaration_snapshots_pkey";

drop table "public"."weight_ticket_declaration_snapshots";

alter table "public"."weight_ticket_lines" add column "declared_weight" numeric;

alter table "public"."weight_ticket_lines" add column "last_declared_at" timestamp with time zone;

CREATE INDEX idx_weight_ticket_lines_declaration_state ON public.weight_ticket_lines USING btree (declared_weight, last_declared_at) WHERE (declared_weight IS NOT NULL);

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


