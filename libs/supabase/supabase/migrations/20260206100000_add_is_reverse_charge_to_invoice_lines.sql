-- Add is_reverse_charge boolean to invoice_lines as a snapshot field
ALTER TABLE "public"."invoice_lines"
  ADD COLUMN "is_reverse_charge" boolean NOT NULL DEFAULT false;

-- Backfill existing rows: set is_reverse_charge = true where vat_code = 'VERLEGD'
UPDATE "public"."invoice_lines"
  SET "is_reverse_charge" = true
  WHERE "vat_code" = 'VERLEGD';
