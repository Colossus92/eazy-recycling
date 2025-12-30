-- Migration: Convert bigint IDs to UUID for multiple tables
-- Tables affected: invoice_lines, invoices, catalog_items, catalog_item_categories, user_roles, weight_ticket_lines, weight_tickets
-- This migration preserves existing data and references

-- ============================================================================
-- STEP 1: Add new UUID columns to all affected tables
-- ============================================================================
BEGIN;
-- Add new UUID columns
ALTER TABLE catalog_item_categories ADD COLUMN IF NOT EXISTS new_id uuid DEFAULT gen_random_uuid();
ALTER TABLE catalog_items ADD COLUMN IF NOT EXISTS new_id uuid DEFAULT gen_random_uuid();
ALTER TABLE catalog_items ADD COLUMN IF NOT EXISTS new_category_id uuid;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS new_id uuid DEFAULT gen_random_uuid();
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS new_original_invoice_id uuid;
ALTER TABLE invoice_lines ADD COLUMN IF NOT EXISTS new_id uuid DEFAULT gen_random_uuid();
ALTER TABLE invoice_lines ADD COLUMN IF NOT EXISTS new_invoice_id uuid;
ALTER TABLE invoice_lines ADD COLUMN IF NOT EXISTS new_catalog_item_id uuid;
ALTER TABLE user_roles ADD COLUMN IF NOT EXISTS new_id uuid DEFAULT gen_random_uuid();
ALTER TABLE weight_ticket_lines ADD COLUMN IF NOT EXISTS new_id uuid DEFAULT gen_random_uuid();
ALTER TABLE weight_ticket_lines ADD COLUMN IF NOT EXISTS new_catalog_item_id uuid;
ALTER TABLE waste_streams ADD COLUMN IF NOT EXISTS new_catalog_item_id uuid;
ALTER TABLE weight_tickets ADD COLUMN IF NOT EXISTS new_linked_invoice_id uuid;
ALTER TABLE weight_tickets ADD COLUMN IF NOT EXISTS new_id uuid DEFAULT gen_random_uuid();
ALTER TABLE weight_ticket_lines ADD COLUMN IF NOT EXISTS new_weight_ticket_id uuid;
ALTER TABLE transports ADD COLUMN IF NOT EXISTS new_weight_ticket_id uuid;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS new_source_weight_ticket_id uuid;

-- ============================================================================
-- STEP 2: Populate new UUID columns with generated UUIDs where needed
-- ============================================================================

-- Generate UUIDs for rows that don't have them yet
UPDATE catalog_item_categories SET new_id = gen_random_uuid() WHERE new_id IS NULL;
UPDATE catalog_items SET new_id = gen_random_uuid() WHERE new_id IS NULL;
UPDATE invoices SET new_id = gen_random_uuid() WHERE new_id IS NULL;
UPDATE invoice_lines SET new_id = gen_random_uuid() WHERE new_id IS NULL;
UPDATE user_roles SET new_id = gen_random_uuid() WHERE new_id IS NULL;
UPDATE weight_ticket_lines SET new_id = gen_random_uuid() WHERE new_id IS NULL;
UPDATE weight_tickets SET new_id = gen_random_uuid() WHERE new_id IS NULL;

-- ============================================================================
-- STEP 3: Update foreign key references to use new UUIDs
-- ============================================================================

-- Update catalog_items.new_category_id to reference catalog_item_categories.new_id
UPDATE catalog_items ci
SET new_category_id = cic.new_id
FROM catalog_item_categories cic
WHERE ci.category_id = cic.id;

-- Update invoices.new_original_invoice_id to reference invoices.new_id (self-reference)
UPDATE invoices i
SET new_original_invoice_id = orig.new_id
FROM invoices orig
WHERE i.original_invoice_id = orig.id;

-- Update invoice_lines.new_invoice_id to reference invoices.new_id
UPDATE invoice_lines il
SET new_invoice_id = inv.new_id
FROM invoices inv
WHERE il.invoice_id = inv.id;

-- Update invoice_lines.new_catalog_item_id to reference catalog_items.new_id
UPDATE invoice_lines il
SET new_catalog_item_id = ci.new_id
FROM catalog_items ci
WHERE il.catalog_item_id = ci.id;

-- Update weight_ticket_lines.new_catalog_item_id to reference catalog_items.new_id
UPDATE weight_ticket_lines wtl
SET new_catalog_item_id = ci.new_id
FROM catalog_items ci
WHERE wtl.catalog_item_id = ci.id;

-- Update waste_streams.new_catalog_item_id to reference catalog_items.new_id
UPDATE waste_streams ws
SET new_catalog_item_id = ci.new_id
FROM catalog_items ci
WHERE ws.catalog_item_id = ci.id;

-- Update weight_tickets.new_linked_invoice_id to reference invoices.new_id
UPDATE weight_tickets wt
SET new_linked_invoice_id = inv.new_id
FROM invoices inv
WHERE wt.linked_invoice_id = inv.id;

-- Update weight_ticket_lines.new_weight_ticket_id to reference weight_tickets.new_id
UPDATE weight_ticket_lines wtl
SET new_weight_ticket_id = wt.new_id
FROM weight_tickets wt
WHERE wtl.weight_ticket_id = wt.id;

-- Update transports.new_weight_ticket_id to reference weight_tickets.new_id
UPDATE transports t
SET new_weight_ticket_id = wt.new_id
FROM weight_tickets wt
WHERE t.weight_ticket_id = wt.id;

-- Update invoices.new_source_weight_ticket_id to reference weight_tickets.new_id
UPDATE invoices t
SET new_source_weight_ticket_id = wt.new_id
FROM weight_tickets wt
WHERE t.source_weight_ticket_id = wt.id;

-- ============================================================================
-- STEP 4: Drop old foreign key constraints
-- ============================================================================

-- Drop foreign key constraints on invoice_lines
ALTER TABLE invoice_lines DROP CONSTRAINT IF EXISTS invoice_lines_invoice_id_fkey;
ALTER TABLE invoice_lines DROP CONSTRAINT IF EXISTS invoice_lines_catalog_item_id_fkey;

-- Drop foreign key constraint on invoices (self-reference)
ALTER TABLE invoices DROP CONSTRAINT IF EXISTS invoices_original_invoice_id_fkey;

-- Drop foreign key constraint on catalog_items
ALTER TABLE catalog_items DROP CONSTRAINT IF EXISTS catalog_items_category_id_fkey;

-- Drop foreign key constraint on weight_ticket_lines (if exists)
ALTER TABLE weight_ticket_lines DROP CONSTRAINT IF EXISTS weight_ticket_lines_catalog_item_id_fkey;
ALTER TABLE weight_ticket_lines DROP CONSTRAINT IF EXISTS fk_weight_ticket_lines_weight_ticket;
ALTER TABLE weight_ticket_lines DROP CONSTRAINT IF EXISTS weight_ticket_lines_weight_ticket_id_fkey;

-- Drop foreign key constraint on waste_streams (if exists)
ALTER TABLE waste_streams DROP CONSTRAINT IF EXISTS waste_streams_catalog_item_id_fkey;

-- Drop foreign key constraint on weight_tickets.linked_invoice_id (THIS MUST BE DROPPED BEFORE invoices_pkey)
ALTER TABLE weight_tickets DROP CONSTRAINT IF EXISTS weight_tickets_linked_invoice_id_fkey;

ALTER TABLE transports DROP CONSTRAINT IF EXISTS transports_weight_ticket_id_fkey;
ALTER TABLE invoices DROP CONSTRAINT IF EXISTS invoices_source_weight_ticket_id_fkey;

-- ============================================================================
-- STEP 5: Drop old primary key constraints
-- ============================================================================

ALTER TABLE catalog_item_categories DROP CONSTRAINT IF EXISTS catalog_item_categories_pkey;
ALTER TABLE catalog_items DROP CONSTRAINT IF EXISTS catalog_items_pkey;
ALTER TABLE invoices DROP CONSTRAINT IF EXISTS invoices_pkey;
ALTER TABLE invoice_lines DROP CONSTRAINT IF EXISTS invoice_lines_pkey;
ALTER TABLE user_roles DROP CONSTRAINT IF EXISTS user_roles_pkey;
ALTER TABLE weight_tickets DROP CONSTRAINT IF EXISTS weight_tickets_pkey;

-- Drop unique constraint on invoice_lines (invoice_id, line_number) if exists
ALTER TABLE invoice_lines DROP CONSTRAINT IF EXISTS invoice_lines_invoice_id_line_number_key;

-- ============================================================================
-- STEP 6: Drop old columns and rename new columns
-- ============================================================================

-- catalog_item_categories
ALTER TABLE catalog_item_categories DROP COLUMN id;
ALTER TABLE catalog_item_categories RENAME COLUMN new_id TO id;

-- catalog_items
ALTER TABLE catalog_items DROP COLUMN id;
ALTER TABLE catalog_items RENAME COLUMN new_id TO id;
ALTER TABLE catalog_items DROP COLUMN category_id;
ALTER TABLE catalog_items RENAME COLUMN new_category_id TO category_id;

-- invoices
ALTER TABLE invoices DROP COLUMN id;
ALTER TABLE invoices RENAME COLUMN new_id TO id;
ALTER TABLE invoices DROP COLUMN original_invoice_id;
ALTER TABLE invoices RENAME COLUMN new_original_invoice_id TO original_invoice_id;

-- invoice_lines
ALTER TABLE invoice_lines DROP COLUMN id;
ALTER TABLE invoice_lines RENAME COLUMN new_id TO id;
ALTER TABLE invoice_lines DROP COLUMN invoice_id;
ALTER TABLE invoice_lines RENAME COLUMN new_invoice_id TO invoice_id;
ALTER TABLE invoice_lines DROP COLUMN catalog_item_id;
ALTER TABLE invoice_lines RENAME COLUMN new_catalog_item_id TO catalog_item_id;

-- user_roles
ALTER TABLE user_roles DROP COLUMN id;
ALTER TABLE user_roles RENAME COLUMN new_id TO id;

-- weight_ticket_lines
ALTER TABLE weight_ticket_lines DROP COLUMN IF EXISTS id;
ALTER TABLE weight_ticket_lines RENAME COLUMN new_id TO id;
ALTER TABLE weight_ticket_lines DROP COLUMN catalog_item_id;
ALTER TABLE weight_ticket_lines RENAME COLUMN new_catalog_item_id TO catalog_item_id;

-- waste_streams
ALTER TABLE waste_streams DROP COLUMN catalog_item_id;
ALTER TABLE waste_streams RENAME COLUMN new_catalog_item_id TO catalog_item_id;

-- weight_tickets: rename id to number, add new UUID id
ALTER TABLE weight_tickets RENAME COLUMN id TO number;
ALTER TABLE weight_tickets RENAME COLUMN new_id TO id;
ALTER TABLE weight_tickets DROP COLUMN linked_invoice_id;
ALTER TABLE weight_tickets RENAME COLUMN new_linked_invoice_id TO linked_invoice_id;

-- weight_ticket_lines: update weight_ticket_id foreign key
ALTER TABLE weight_ticket_lines DROP COLUMN weight_ticket_id;
ALTER TABLE weight_ticket_lines RENAME COLUMN new_weight_ticket_id TO weight_ticket_id;

ALTER TABLE transports DROP COLUMN weight_ticket_id;
ALTER TABLE transports RENAME COLUMN new_weight_ticket_id TO weight_ticket_id;

ALTER TABLE invoices DROP COLUMN source_weight_ticket_id;
ALTER TABLE invoices RENAME COLUMN new_source_weight_ticket_id TO source_weight_ticket_id;

-- ============================================================================
-- STEP 7: Add NOT NULL constraints where required
-- ============================================================================

ALTER TABLE catalog_item_categories ALTER COLUMN id SET NOT NULL;
ALTER TABLE catalog_items ALTER COLUMN id SET NOT NULL;
ALTER TABLE invoices ALTER COLUMN id SET NOT NULL;
ALTER TABLE invoice_lines ALTER COLUMN id SET NOT NULL;
ALTER TABLE invoice_lines ALTER COLUMN invoice_id SET NOT NULL;
ALTER TABLE user_roles ALTER COLUMN id SET NOT NULL;
ALTER TABLE weight_ticket_lines ALTER COLUMN id SET NOT NULL;
ALTER TABLE weight_ticket_lines ALTER COLUMN catalog_item_id SET NOT NULL;
ALTER TABLE weight_ticket_lines ALTER COLUMN weight_ticket_id SET NOT NULL;
ALTER TABLE weight_tickets ALTER COLUMN id SET NOT NULL;
ALTER TABLE weight_tickets ALTER COLUMN number SET NOT NULL;

-- ============================================================================
-- STEP 8: Recreate primary key constraints
-- ============================================================================

ALTER TABLE catalog_item_categories ADD PRIMARY KEY (id);
ALTER TABLE catalog_items ADD PRIMARY KEY (id);
ALTER TABLE invoices ADD PRIMARY KEY (id);
ALTER TABLE invoice_lines ADD PRIMARY KEY (id);
ALTER TABLE user_roles ADD PRIMARY KEY (id);
ALTER TABLE weight_ticket_lines ADD PRIMARY KEY (id);
ALTER TABLE weight_tickets ADD PRIMARY KEY (id);
ALTER TABLE weight_tickets ADD CONSTRAINT weight_tickets_number_unique UNIQUE (number);

-- ============================================================================
-- STEP 9: Recreate foreign key constraints
-- ============================================================================

ALTER TABLE catalog_items
  ADD CONSTRAINT catalog_items_category_id_fkey
  FOREIGN KEY (category_id) REFERENCES catalog_item_categories(id);

ALTER TABLE invoices
  ADD CONSTRAINT invoices_original_invoice_id_fkey
  FOREIGN KEY (original_invoice_id) REFERENCES invoices(id);

ALTER TABLE invoice_lines
  ADD CONSTRAINT invoice_lines_invoice_id_fkey
  FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE;

ALTER TABLE invoice_lines
  ADD CONSTRAINT invoice_lines_catalog_item_id_fkey
  FOREIGN KEY (catalog_item_id) REFERENCES catalog_items(id);

ALTER TABLE weight_ticket_lines
  ADD CONSTRAINT weight_ticket_lines_catalog_item_id_fkey
  FOREIGN KEY (catalog_item_id) REFERENCES catalog_items(id);

ALTER TABLE weight_ticket_lines
  ADD CONSTRAINT fk_weight_ticket_lines_weight_ticket
  FOREIGN KEY (weight_ticket_id) REFERENCES weight_tickets(id);

ALTER TABLE waste_streams
  ADD CONSTRAINT waste_streams_catalog_item_id_fkey
  FOREIGN KEY (catalog_item_id) REFERENCES catalog_items(id);

ALTER TABLE weight_tickets
  ADD CONSTRAINT weight_tickets_linked_invoice_id_fkey
  FOREIGN KEY (linked_invoice_id) REFERENCES invoices(id);

ALTER TABLE transports
  ADD CONSTRAINT transports_weight_ticket_id_fkey
  FOREIGN KEY (weight_ticket_id) REFERENCES weight_tickets(id);

ALTER TABLE invoices
  ADD CONSTRAINT invoices_source_weight_ticket_id_fkey
  FOREIGN KEY (source_weight_ticket_id) REFERENCES weight_tickets(id);

-- ============================================================================
-- STEP 10: Recreate unique constraints
-- ============================================================================

ALTER TABLE invoice_lines
  ADD CONSTRAINT invoice_lines_invoice_id_line_number_key
  UNIQUE (invoice_id, line_number);

-- ============================================================================
-- STEP 11: Drop old sequences (no longer needed)
-- ============================================================================

DROP SEQUENCE IF EXISTS catalog_item_categories_id_seq;
DROP SEQUENCE IF EXISTS catalog_items_id_seq;
DROP SEQUENCE IF EXISTS invoices_id_seq;
DROP SEQUENCE IF EXISTS invoice_lines_id_seq;

-- ============================================================================
-- STEP 12: Update any indexes that reference old columns
-- ============================================================================

-- Recreate any necessary indexes on the new UUID columns
CREATE INDEX IF NOT EXISTS idx_invoice_lines_invoice_id ON invoice_lines(invoice_id);
CREATE INDEX IF NOT EXISTS idx_invoice_lines_catalog_item_id ON invoice_lines(catalog_item_id);
CREATE INDEX IF NOT EXISTS idx_catalog_items_category_id ON catalog_items(category_id);
CREATE INDEX IF NOT EXISTS idx_weight_ticket_lines_catalog_item_id ON weight_ticket_lines(catalog_item_id);

-- ============================================================================
-- Migration complete!
-- ============================================================================
COMMIT;
