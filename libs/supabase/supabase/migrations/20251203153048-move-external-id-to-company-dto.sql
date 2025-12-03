-- Migration: Move external_id from companies_sync to companies as 'code'
-- This migration:
-- 1. Adds a 'code' column to the companies table
-- 2. Migrates data from companies_sync.external_id to companies.code
-- 3. Removes the external_id column from companies_sync

-- Step 1: Add 'code' column to companies table
ALTER TABLE companies ADD COLUMN code TEXT;

-- Step 2: Migrate data from companies_sync.external_id to companies.code
-- Join on company_id to copy the external_id values, trimming whitespace
UPDATE companies c
SET code = TRIM(cs.external_id)
FROM companies_sync cs
WHERE c.id = cs.company_id
  AND cs.external_id IS NOT NULL;

-- Step 3: Drop the external_id column from companies_sync
ALTER TABLE companies_sync DROP COLUMN external_id;

-- Optional: Create an index on code for better query performance
CREATE INDEX idx_companies_code ON companies(code);
