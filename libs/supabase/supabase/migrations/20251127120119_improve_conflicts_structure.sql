alter table "public"."companies_sync" drop constraint "exact_sync_company_id_fkey";

alter table "public"."companies_sync" drop column "requires_manual_review";

alter table "public"."companies_sync" alter column "company_id" drop not null;

alter table "public"."companies_sync" add constraint "exact_sync_company_id_fkey" FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE SET NULL not valid;

alter table "public"."companies_sync" validate constraint "exact_sync_company_id_fkey";


