-- Remove unique constraint on chamber_of_commerce_id
-- This allows multiple companies to have the same KVK number, matching Exact Online's behavior.
-- See ADR-0018 for the rationale behind this decision.

ALTER TABLE "public"."companies"
    DROP CONSTRAINT IF EXISTS "companies_chamber_of_commerce_id_key";

-- Add an index for performance on KVK lookups (non-unique)
CREATE INDEX IF NOT EXISTS "idx_companies_chamber_of_commerce_id"
    ON "public"."companies" ("chamber_of_commerce_id")
    WHERE "chamber_of_commerce_id" IS NOT NULL AND "deleted_at" IS NULL;
