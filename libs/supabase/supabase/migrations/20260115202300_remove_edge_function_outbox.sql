-- Migration: Remove edge_function_outbox table and related objects
-- Reason: Replaced custom outbox pattern with Jobrunr for background job processing
-- Date: 2026-01-15

-- Drop indexes
DROP INDEX IF EXISTS "public"."idx_edge_function_outbox_status";
DROP INDEX IF EXISTS "public"."idx_edge_function_outbox_created_at";

-- Drop table (this will also drop the primary key constraint)
DROP TABLE IF EXISTS "public"."edge_function_outbox";

-- Drop sequence
DROP SEQUENCE IF EXISTS "public"."edge_function_outbox_id_seq";

-- Note: RLS policies and grants are automatically removed when the table is dropped
