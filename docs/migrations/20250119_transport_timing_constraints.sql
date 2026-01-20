-- Migration: Add timing constraint fields to transports table
-- Date: 2025-01-19
-- Description: Replace pickup_date_time and delivery_date_time with structured timing constraint fields
--              to support DATE_ONLY, WINDOW, and FIXED timing modes for VRPTW optimization.

-- Step 1: Add new timing constraint columns for pickup
ALTER TABLE transports
ADD COLUMN IF NOT EXISTS pickup_date date,
ADD COLUMN IF NOT EXISTS pickup_timing_mode text,
ADD COLUMN IF NOT EXISTS pickup_window_start time,
ADD COLUMN IF NOT EXISTS pickup_window_end time;

-- Step 2: Add new timing constraint columns for delivery
ALTER TABLE transports
ADD COLUMN IF NOT EXISTS delivery_date date,
ADD COLUMN IF NOT EXISTS delivery_timing_mode text,
ADD COLUMN IF NOT EXISTS delivery_window_start time,
ADD COLUMN IF NOT EXISTS delivery_window_end time;

-- Step 3: Migrate existing data from pickup_date_time to new columns
-- Convert existing timestamps to FIXED timing mode with the same time for start and end
UPDATE transports
SET
  pickup_date = (pickup_date_time AT TIME ZONE 'Europe/Amsterdam')::date,
  pickup_timing_mode = 'FIXED',
  pickup_window_start = (pickup_date_time AT TIME ZONE 'Europe/Amsterdam')::time,
  pickup_window_end = (pickup_date_time AT TIME ZONE 'Europe/Amsterdam')::time
WHERE pickup_date_time IS NOT NULL AND pickup_date IS NULL;

-- Step 4: Migrate existing data from delivery_date_time to new columns
UPDATE transports
SET
  delivery_date = (delivery_date_time AT TIME ZONE 'Europe/Amsterdam')::date,
  delivery_timing_mode = 'FIXED',
  delivery_window_start = (delivery_date_time AT TIME ZONE 'Europe/Amsterdam')::time,
  delivery_window_end = (delivery_date_time AT TIME ZONE 'Europe/Amsterdam')::time
WHERE delivery_date_time IS NOT NULL AND delivery_date IS NULL;

-- Step 5: Make pickup_date and pickup_timing_mode NOT NULL after migration
-- (Only run this after verifying all data has been migrated)
-- ALTER TABLE transports ALTER COLUMN pickup_date SET NOT NULL;
-- ALTER TABLE transports ALTER COLUMN pickup_timing_mode SET NOT NULL;

-- Step 6: Add check constraint for timing mode values
ALTER TABLE transports
DROP CONSTRAINT IF EXISTS chk_pickup_timing_mode;

ALTER TABLE transports
DROP CONSTRAINT IF EXISTS chk_delivery_timing_mode;

-- Step 7: (Optional) Drop old columns after confirming migration success
-- WARNING: Only run this after verifying the application works correctly with new columns
-- ALTER TABLE transports DROP COLUMN IF EXISTS pickup_date_time;
-- ALTER TABLE transports DROP COLUMN IF EXISTS delivery_date_time;

-- Verification query: Check migration results
-- SELECT
--   id,
--   pickup_date_time,
--   pickup_date,
--   pickup_timing_mode,
--   pickup_window_start,
--   pickup_window_end,
--   delivery_date_time,
--   delivery_date,
--   delivery_timing_mode,
--   delivery_window_start,
--   delivery_window_end
-- FROM transports
-- LIMIT 10;
