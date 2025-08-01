alter table "public"."transports" add column "delivery_company_branch_id" uuid;

alter table "public"."transports" add column "pickup_company_branch_id" uuid;

alter table "public"."transports" add constraint "transports_delivery_company_branch_id_fkey" FOREIGN KEY (delivery_company_branch_id) REFERENCES company_branches(id) not valid;

alter table "public"."transports" validate constraint "transports_delivery_company_branch_id_fkey";

alter table "public"."transports" add constraint "transports_pickup_company_branch_id_fkey" FOREIGN KEY (pickup_company_branch_id) REFERENCES company_branches(id) not valid;

alter table "public"."transports" validate constraint "transports_pickup_company_branch_id_fkey";


