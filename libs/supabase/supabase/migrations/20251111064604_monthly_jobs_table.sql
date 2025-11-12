
  create table "public"."lma_declaration_sessions" (
    "id" uuid not null,
    "type" text not null,
    "declaration_ids" text[] not null,
    "status" text not null,
    "errors" text[],
    "created_at" timestamp with time zone not null,
    "processed_at" timestamp with time zone
      );


alter table "public"."lma_declaration_sessions" enable row level security;


  create table "public"."lma_declarations" (
    "id" text not null,
    "waste_stream_number" text not null,
    "period" text not null,
    "transporters" text[] not null,
    "total_weight" bigint not null,
    "total_shipments" bigint not null,
    "created_at" timestamp with time zone not null default now()
      );


alter table "public"."lma_declarations" enable row level security;


  create table "public"."monthly_waste_declaration_jobs" (
    "id" uuid not null,
    "job_type" text not null,
    "year_month" text not null,
    "status" text not null,
    "created_at" timestamp with time zone not null,
    "fulfilled_at" timestamp with time zone
      );


alter table "public"."monthly_waste_declaration_jobs" enable row level security;

CREATE UNIQUE INDEX lma_declaration_sessions_pkey ON public.lma_declaration_sessions USING btree (id);

CREATE UNIQUE INDEX lma_declarations_pkey ON public.lma_declarations USING btree (id);

CREATE UNIQUE INDEX monthly_waste_declaration_jobs_pkey ON public.monthly_waste_declaration_jobs USING btree (id);

alter table "public"."lma_declaration_sessions" add constraint "lma_declaration_sessions_pkey" PRIMARY KEY using index "lma_declaration_sessions_pkey";

alter table "public"."lma_declarations" add constraint "lma_declarations_pkey" PRIMARY KEY using index "lma_declarations_pkey";

alter table "public"."monthly_waste_declaration_jobs" add constraint "monthly_waste_declaration_jobs_pkey" PRIMARY KEY using index "monthly_waste_declaration_jobs_pkey";

alter table "public"."lma_declarations" add constraint "lma_declarations_waste_stream_number_fkey" FOREIGN KEY (waste_stream_number) REFERENCES public.waste_streams(number) not valid;

alter table "public"."lma_declarations" validate constraint "lma_declarations_waste_stream_number_fkey";


grant delete on table "public"."lma_declaration_sessions" to "anon";

grant insert on table "public"."lma_declaration_sessions" to "anon";

grant references on table "public"."lma_declaration_sessions" to "anon";

grant select on table "public"."lma_declaration_sessions" to "anon";

grant trigger on table "public"."lma_declaration_sessions" to "anon";

grant truncate on table "public"."lma_declaration_sessions" to "anon";

grant update on table "public"."lma_declaration_sessions" to "anon";

grant delete on table "public"."lma_declaration_sessions" to "authenticated";

grant insert on table "public"."lma_declaration_sessions" to "authenticated";

grant references on table "public"."lma_declaration_sessions" to "authenticated";

grant select on table "public"."lma_declaration_sessions" to "authenticated";

grant trigger on table "public"."lma_declaration_sessions" to "authenticated";

grant truncate on table "public"."lma_declaration_sessions" to "authenticated";

grant update on table "public"."lma_declaration_sessions" to "authenticated";

grant delete on table "public"."lma_declaration_sessions" to "service_role";

grant insert on table "public"."lma_declaration_sessions" to "service_role";

grant references on table "public"."lma_declaration_sessions" to "service_role";

grant select on table "public"."lma_declaration_sessions" to "service_role";

grant trigger on table "public"."lma_declaration_sessions" to "service_role";

grant truncate on table "public"."lma_declaration_sessions" to "service_role";

grant update on table "public"."lma_declaration_sessions" to "service_role";

grant delete on table "public"."lma_declarations" to "anon";

grant insert on table "public"."lma_declarations" to "anon";

grant references on table "public"."lma_declarations" to "anon";

grant select on table "public"."lma_declarations" to "anon";

grant trigger on table "public"."lma_declarations" to "anon";

grant truncate on table "public"."lma_declarations" to "anon";

grant update on table "public"."lma_declarations" to "anon";

grant delete on table "public"."lma_declarations" to "authenticated";

grant insert on table "public"."lma_declarations" to "authenticated";

grant references on table "public"."lma_declarations" to "authenticated";

grant select on table "public"."lma_declarations" to "authenticated";

grant trigger on table "public"."lma_declarations" to "authenticated";

grant truncate on table "public"."lma_declarations" to "authenticated";

grant update on table "public"."lma_declarations" to "authenticated";

grant delete on table "public"."lma_declarations" to "service_role";

grant insert on table "public"."lma_declarations" to "service_role";

grant references on table "public"."lma_declarations" to "service_role";

grant select on table "public"."lma_declarations" to "service_role";

grant trigger on table "public"."lma_declarations" to "service_role";

grant truncate on table "public"."lma_declarations" to "service_role";

grant update on table "public"."lma_declarations" to "service_role";

grant delete on table "public"."monthly_waste_declaration_jobs" to "anon";

grant insert on table "public"."monthly_waste_declaration_jobs" to "anon";

grant references on table "public"."monthly_waste_declaration_jobs" to "anon";

grant select on table "public"."monthly_waste_declaration_jobs" to "anon";

grant trigger on table "public"."monthly_waste_declaration_jobs" to "anon";

grant truncate on table "public"."monthly_waste_declaration_jobs" to "anon";

grant update on table "public"."monthly_waste_declaration_jobs" to "anon";

grant delete on table "public"."monthly_waste_declaration_jobs" to "authenticated";

grant insert on table "public"."monthly_waste_declaration_jobs" to "authenticated";

grant references on table "public"."monthly_waste_declaration_jobs" to "authenticated";

grant select on table "public"."monthly_waste_declaration_jobs" to "authenticated";

grant trigger on table "public"."monthly_waste_declaration_jobs" to "authenticated";

grant truncate on table "public"."monthly_waste_declaration_jobs" to "authenticated";

grant update on table "public"."monthly_waste_declaration_jobs" to "authenticated";

grant delete on table "public"."monthly_waste_declaration_jobs" to "service_role";

grant insert on table "public"."monthly_waste_declaration_jobs" to "service_role";

grant references on table "public"."monthly_waste_declaration_jobs" to "service_role";

grant select on table "public"."monthly_waste_declaration_jobs" to "service_role";

grant trigger on table "public"."monthly_waste_declaration_jobs" to "service_role";

grant truncate on table "public"."monthly_waste_declaration_jobs" to "service_role";

grant update on table "public"."monthly_waste_declaration_jobs" to "service_role";
