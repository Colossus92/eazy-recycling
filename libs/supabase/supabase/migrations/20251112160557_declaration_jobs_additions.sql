create sequence "public"."receival_declaration_id_seq";

alter table "public"."lma_declarations" add column "amice_uuid" uuid;

alter table "public"."lma_declarations" add column "errors" text[];

alter table "public"."lma_declarations" add column "status" text not null;

alter table "public"."weight_tickets" add column "second_weighing_unit" text;

alter table "public"."weight_tickets" add column "second_weighing_value" numeric;

CREATE UNIQUE INDEX lma_declarations_amice_uuid_key ON public.lma_declarations USING btree (amice_uuid);

alter table "public"."lma_declarations" add constraint "lma_declarations_amice_uuid_key" UNIQUE using index "lma_declarations_amice_uuid_key";
