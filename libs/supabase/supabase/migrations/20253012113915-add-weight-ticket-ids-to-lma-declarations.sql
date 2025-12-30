-- Migration: Add weight_ticket_ids column to lma_declarations table
-- This column stores the UUIDs of weight tickets included in late declarations
-- to enable marking them as declared after approval

ALTER TABLE lma_declarations
ADD COLUMN IF NOT EXISTS weight_ticket_ids uuid[];
