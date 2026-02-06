-- Add UUID id column to vat_rates table and migrate catalog_items FK

-- 1. Add id column to vat_rates
ALTER TABLE vat_rates
ADD COLUMN id UUID DEFAULT gen_random_uuid() NOT NULL;

-- 2. Populate id for existing rows
UPDATE vat_rates SET id = gen_random_uuid() WHERE id IS NULL;

-- 3. Add vat_rate_id column to catalog_items (before PK swap, while vat_code FK still works)
ALTER TABLE catalog_items
ADD COLUMN vat_rate_id UUID;

-- 4. Populate vat_rate_id from existing vat_code join
UPDATE catalog_items ci
SET vat_rate_id = vr.id
FROM vat_rates vr
WHERE ci.vat_code = vr.vat_code;

-- 5. Make vat_rate_id NOT NULL now that it's populated
ALTER TABLE catalog_items
ALTER COLUMN vat_rate_id SET NOT NULL;

-- 6. Drop old vat_code FK from catalog_items (must drop before changing vat_rates PK)
ALTER TABLE catalog_items
DROP CONSTRAINT catalog_items_vat_code_fkey;

-- 7. Drop old vat_code column from catalog_items (no longer needed as FK)
ALTER TABLE catalog_items
DROP COLUMN vat_code;

-- 8. Swap primary key: drop old vat_code PK, add id as new PK
ALTER TABLE vat_rates
DROP CONSTRAINT vat_rates_pkey;

ALTER TABLE vat_rates
ADD CONSTRAINT vat_rates_pkey PRIMARY KEY (id);

-- 9. Add unique constraint on vat_code (preserve lookup integrity)
ALTER TABLE vat_rates
ADD CONSTRAINT vat_rates_vat_code_unique UNIQUE (vat_code);

-- 10. Add FK constraint from catalog_items to vat_rates(id)
ALTER TABLE catalog_items
ADD CONSTRAINT catalog_items_vat_rate_id_fkey
FOREIGN KEY (vat_rate_id) REFERENCES vat_rates(id);

-- 11. Move all MATERIAL catalog items to use the VERLEGD vat rate
UPDATE catalog_items
SET vat_rate_id = (SELECT id FROM vat_rates WHERE vat_code = 'VERLEGD')
WHERE type = 'MATERIAL';

-- 12. Add column documentation
COMMENT ON COLUMN vat_rates.id IS 'Unique identifier for the VAT rate. Used as FK reference from other tables.';
COMMENT ON COLUMN catalog_items.vat_rate_id IS 'Reference to the VAT rate applied to this catalog item.';
