alter table "public"."transports" add column "weight_ticket_id" bigint;

alter table "public"."transports" add constraint "transports_weight_ticket_id_fkey" FOREIGN KEY (weight_ticket_id) REFERENCES public.weight_tickets(id) not valid;

alter table "public"."transports" validate constraint "transports_weight_ticket_id_fkey";


