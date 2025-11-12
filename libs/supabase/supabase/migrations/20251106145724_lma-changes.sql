alter table "public"."waste_containers" drop constraint "waste_containers_location_company_id_fkey";

alter table "public"."waste_containers" drop column "building_name";

alter table "public"."waste_containers" drop column "building_number";

alter table "public"."waste_containers" drop column "city";

alter table "public"."waste_containers" drop column "country";

alter table "public"."waste_containers" drop column "location_company_id";

alter table "public"."waste_containers" drop column "postal_code";

alter table "public"."waste_containers" drop column "street_name";

alter table "public"."waste_containers" drop column "type";


