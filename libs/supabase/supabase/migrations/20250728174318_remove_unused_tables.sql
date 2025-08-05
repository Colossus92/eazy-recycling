drop policy "Enable all access for authenticated users only" on "public"."trips";

revoke delete on table "public"."trips" from "anon";

revoke insert on table "public"."trips" from "anon";

revoke references on table "public"."trips" from "anon";

revoke select on table "public"."trips" from "anon";

revoke trigger on table "public"."trips" from "anon";

revoke truncate on table "public"."trips" from "anon";

revoke update on table "public"."trips" from "anon";

revoke delete on table "public"."trips" from "authenticated";

revoke insert on table "public"."trips" from "authenticated";

revoke references on table "public"."trips" from "authenticated";

revoke select on table "public"."trips" from "authenticated";

revoke trigger on table "public"."trips" from "authenticated";

revoke truncate on table "public"."trips" from "authenticated";

revoke update on table "public"."trips" from "authenticated";

revoke delete on table "public"."trips" from "service_role";

revoke insert on table "public"."trips" from "service_role";

revoke references on table "public"."trips" from "service_role";

revoke select on table "public"."trips" from "service_role";

revoke trigger on table "public"."trips" from "service_role";

revoke truncate on table "public"."trips" from "service_role";

revoke update on table "public"."trips" from "service_role";

revoke delete on table "public"."waybills" from "anon";

revoke insert on table "public"."waybills" from "anon";

revoke references on table "public"."waybills" from "anon";

revoke select on table "public"."waybills" from "anon";

revoke trigger on table "public"."waybills" from "anon";

revoke truncate on table "public"."waybills" from "anon";

revoke update on table "public"."waybills" from "anon";

revoke delete on table "public"."waybills" from "authenticated";

revoke insert on table "public"."waybills" from "authenticated";

revoke references on table "public"."waybills" from "authenticated";

revoke select on table "public"."waybills" from "authenticated";

revoke trigger on table "public"."waybills" from "authenticated";

revoke truncate on table "public"."waybills" from "authenticated";

revoke update on table "public"."waybills" from "authenticated";

revoke delete on table "public"."waybills" from "service_role";

revoke insert on table "public"."waybills" from "service_role";

revoke references on table "public"."waybills" from "service_role";

revoke select on table "public"."waybills" from "service_role";

revoke trigger on table "public"."waybills" from "service_role";

revoke truncate on table "public"."waybills" from "service_role";

revoke update on table "public"."waybills" from "service_role";

alter table "public"."trips" drop constraint "trips_destination_company_id_fkey";

alter table "public"."trips" drop constraint "trips_driver_id_fkey1";

alter table "public"."trips" drop constraint "trips_origin_company_id_fkey";

alter table "public"."trips" drop constraint "trips_truck_license_plate_fkey";

alter table "public"."waybills" drop constraint "fk3x2ksx5hynkablsj7k46cg1qs";

alter table "public"."waybills" drop constraint "fk6wh6ln69550kuiji853lvwitl";

alter table "public"."waybills" drop constraint "fkexb5sdt0oqb9a6ehk1f05krmv";

alter table "public"."waybills" drop constraint "fkh7c49u3crn1swqeoton0umk4x";

alter table "public"."waybills" drop constraint "fkhaxs4v1au4fk8sahm9map6j49";

alter table "public"."waybills" drop constraint "fksrjknpahp89cmae80n86m0hsy";

alter table "public"."waybills" drop constraint "fkswgts2ytehbdal35brikeaexc";

alter table "public"."waybills" drop constraint "waybills_goods_item_id_key";

alter table "public"."trips" drop constraint "trips_pkey";

alter table "public"."waybills" drop constraint "waybills_pkey";

drop index if exists "public"."trips_pkey";

drop index if exists "public"."waybills_goods_item_id_key";

drop index if exists "public"."waybills_pkey";

drop table "public"."trips";

drop table "public"."waybills";


