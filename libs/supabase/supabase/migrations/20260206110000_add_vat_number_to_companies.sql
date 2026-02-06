-- Add vat_number column to companies table (synced from Exact Online VATNumber field)
ALTER TABLE "public"."companies"
  ADD COLUMN "vat_number" text;
