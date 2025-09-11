alter table "public"."transports" add column "transport_hours" numeric(3,1) default NULL::numeric;

alter table "public"."transports" add constraint "transports_transport_hours_check" CHECK (((transport_hours IS NULL) OR (transport_hours >= (0)::numeric))) not valid;

alter table "public"."transports" validate constraint "transports_transport_hours_check";


