revoke delete on table "public"."material_groups" from "anon";

revoke insert on table "public"."material_groups" from "anon";

revoke references on table "public"."material_groups" from "anon";

revoke select on table "public"."material_groups" from "anon";

revoke trigger on table "public"."material_groups" from "anon";

revoke truncate on table "public"."material_groups" from "anon";

revoke update on table "public"."material_groups" from "anon";

revoke delete on table "public"."material_groups" from "authenticated";

revoke insert on table "public"."material_groups" from "authenticated";

revoke references on table "public"."material_groups" from "authenticated";

revoke select on table "public"."material_groups" from "authenticated";

revoke trigger on table "public"."material_groups" from "authenticated";

revoke truncate on table "public"."material_groups" from "authenticated";

revoke update on table "public"."material_groups" from "authenticated";

revoke delete on table "public"."material_groups" from "service_role";

revoke insert on table "public"."material_groups" from "service_role";

revoke references on table "public"."material_groups" from "service_role";

revoke select on table "public"."material_groups" from "service_role";

revoke trigger on table "public"."material_groups" from "service_role";

revoke truncate on table "public"."material_groups" from "service_role";

revoke update on table "public"."material_groups" from "service_role";

revoke delete on table "public"."material_prices" from "anon";

revoke insert on table "public"."material_prices" from "anon";

revoke references on table "public"."material_prices" from "anon";

revoke select on table "public"."material_prices" from "anon";

revoke trigger on table "public"."material_prices" from "anon";

revoke truncate on table "public"."material_prices" from "anon";

revoke update on table "public"."material_prices" from "anon";

revoke delete on table "public"."material_prices" from "authenticated";

revoke insert on table "public"."material_prices" from "authenticated";

revoke references on table "public"."material_prices" from "authenticated";

revoke select on table "public"."material_prices" from "authenticated";

revoke trigger on table "public"."material_prices" from "authenticated";

revoke truncate on table "public"."material_prices" from "authenticated";

revoke update on table "public"."material_prices" from "authenticated";

revoke delete on table "public"."material_prices" from "service_role";

revoke insert on table "public"."material_prices" from "service_role";

revoke references on table "public"."material_prices" from "service_role";

revoke select on table "public"."material_prices" from "service_role";

revoke trigger on table "public"."material_prices" from "service_role";

revoke truncate on table "public"."material_prices" from "service_role";

revoke update on table "public"."material_prices" from "service_role";

revoke delete on table "public"."materials" from "anon";

revoke insert on table "public"."materials" from "anon";

revoke references on table "public"."materials" from "anon";

revoke select on table "public"."materials" from "anon";

revoke trigger on table "public"."materials" from "anon";

revoke truncate on table "public"."materials" from "anon";

revoke update on table "public"."materials" from "anon";

revoke delete on table "public"."materials" from "authenticated";

revoke insert on table "public"."materials" from "authenticated";

revoke references on table "public"."materials" from "authenticated";

revoke select on table "public"."materials" from "authenticated";

revoke trigger on table "public"."materials" from "authenticated";

revoke truncate on table "public"."materials" from "authenticated";

revoke update on table "public"."materials" from "authenticated";

revoke delete on table "public"."materials" from "service_role";

revoke insert on table "public"."materials" from "service_role";

revoke references on table "public"."materials" from "service_role";

revoke select on table "public"."materials" from "service_role";

revoke trigger on table "public"."materials" from "service_role";

revoke truncate on table "public"."materials" from "service_role";

revoke update on table "public"."materials" from "service_role";

revoke delete on table "public"."product_categories" from "anon";

revoke insert on table "public"."product_categories" from "anon";

revoke references on table "public"."product_categories" from "anon";

revoke select on table "public"."product_categories" from "anon";

revoke trigger on table "public"."product_categories" from "anon";

revoke truncate on table "public"."product_categories" from "anon";

revoke update on table "public"."product_categories" from "anon";

revoke delete on table "public"."product_categories" from "authenticated";

revoke insert on table "public"."product_categories" from "authenticated";

revoke references on table "public"."product_categories" from "authenticated";

revoke select on table "public"."product_categories" from "authenticated";

revoke trigger on table "public"."product_categories" from "authenticated";

revoke truncate on table "public"."product_categories" from "authenticated";

revoke update on table "public"."product_categories" from "authenticated";

revoke delete on table "public"."product_categories" from "service_role";

revoke insert on table "public"."product_categories" from "service_role";

revoke references on table "public"."product_categories" from "service_role";

revoke select on table "public"."product_categories" from "service_role";

revoke trigger on table "public"."product_categories" from "service_role";

revoke truncate on table "public"."product_categories" from "service_role";

revoke update on table "public"."product_categories" from "service_role";

revoke delete on table "public"."products" from "anon";

revoke insert on table "public"."products" from "anon";

revoke references on table "public"."products" from "anon";

revoke select on table "public"."products" from "anon";

revoke trigger on table "public"."products" from "anon";

revoke truncate on table "public"."products" from "anon";

revoke update on table "public"."products" from "anon";

revoke delete on table "public"."products" from "authenticated";

revoke insert on table "public"."products" from "authenticated";

revoke references on table "public"."products" from "authenticated";

revoke select on table "public"."products" from "authenticated";

revoke trigger on table "public"."products" from "authenticated";

revoke truncate on table "public"."products" from "authenticated";

revoke update on table "public"."products" from "authenticated";

revoke delete on table "public"."products" from "service_role";

revoke insert on table "public"."products" from "service_role";

revoke references on table "public"."products" from "service_role";

revoke select on table "public"."products" from "service_role";

revoke trigger on table "public"."products" from "service_role";

revoke truncate on table "public"."products" from "service_role";

revoke update on table "public"."products" from "service_role";

alter table "public"."material_groups" drop constraint "material_group_code_key";

alter table "public"."material_prices" drop constraint "material_prices_material_id_fkey";

alter table "public"."materials" drop constraint "materials_code_key";

alter table "public"."product_categories" drop constraint "product_categories_code_key";

alter table "public"."products" drop constraint "products_code_key";

alter table "public"."material_groups" drop constraint "material_group_pkey";

alter table "public"."material_prices" drop constraint "material_prices_pkey";

alter table "public"."materials" drop constraint "materials_pkey";

alter table "public"."product_categories" drop constraint "product_categories_pkey";

drop index if exists "public"."material_group_code_key";

drop index if exists "public"."material_group_pkey";

drop index if exists "public"."material_prices_pkey";

drop index if exists "public"."materials_code_key";

drop index if exists "public"."materials_pkey";

drop index if exists "public"."product_categories_code_key";

drop index if exists "public"."product_categories_pkey";

drop index if exists "public"."products_code_key";

drop table "public"."material_groups";

drop table "public"."material_prices";

drop table "public"."materials";

drop table "public"."product_categories";

drop table "public"."products";

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


