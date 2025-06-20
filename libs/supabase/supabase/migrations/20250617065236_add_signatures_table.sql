create table "public"."signatures" (
    "transport_id" uuid not null,
    "consignor_signature" text,
    "consignor_email" text,
    "consignor_signed_at" timestamp with time zone,
    "pickup_signature" text,
    "pickup_email" text,
    "pickup_signed_at" timestamp with time zone,
    "carrier_signature" text,
    "carrier_email" text,
    "carrier_signed_at" timestamp with time zone,
    "consignee_signature" text,
    "consignee_email" text,
    "consignee_signed_at" text
);


alter table "public"."signatures" enable row level security;

CREATE UNIQUE INDEX signatures_pkey ON public.signatures USING btree (transport_id);

alter table "public"."signatures" add constraint "signatures_pkey" PRIMARY KEY using index "signatures_pkey";

alter table "public"."signatures" add constraint "signatures_transport_id_fkey" FOREIGN KEY (transport_id) REFERENCES transports(id) not valid;

alter table "public"."signatures" validate constraint "signatures_transport_id_fkey";

grant delete on table "public"."signatures" to "anon";

grant insert on table "public"."signatures" to "anon";

grant references on table "public"."signatures" to "anon";

grant select on table "public"."signatures" to "anon";

grant trigger on table "public"."signatures" to "anon";

grant truncate on table "public"."signatures" to "anon";

grant update on table "public"."signatures" to "anon";

grant delete on table "public"."signatures" to "authenticated";

grant insert on table "public"."signatures" to "authenticated";

grant references on table "public"."signatures" to "authenticated";

grant select on table "public"."signatures" to "authenticated";

grant trigger on table "public"."signatures" to "authenticated";

grant truncate on table "public"."signatures" to "authenticated";

grant update on table "public"."signatures" to "authenticated";

grant delete on table "public"."signatures" to "service_role";

grant insert on table "public"."signatures" to "service_role";

grant references on table "public"."signatures" to "service_role";

grant select on table "public"."signatures" to "service_role";

grant trigger on table "public"."signatures" to "service_role";

grant truncate on table "public"."signatures" to "service_role";

grant update on table "public"."signatures" to "service_role";


