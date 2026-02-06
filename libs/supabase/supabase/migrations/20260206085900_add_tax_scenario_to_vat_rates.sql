-- Add tax_scenario column to vat_rates table
-- This column indicates the tax scenario for the VAT rate (e.g., STANDARD, REVERSE_CHARGE)

-- Add the tax_scenario column with default value 'STANDARD'
ALTER TABLE vat_rates
ADD COLUMN tax_scenario TEXT NOT NULL DEFAULT 'STANDARD';

-- Add column documentation
COMMENT ON COLUMN vat_rates.tax_scenario IS 'Tax scenario for the VAT rate (e.g., STANDARD, REVERSE_CHARGE). Determines how the VAT is applied in different business contexts.';

-- Insert the reverse charge VAT rate
INSERT INTO vat_rates (vat_code, percentage, valid_from, valid_to, country_code, description, tax_scenario, created_at, created_by, last_modified_at, last_modified_by)
VALUES ('VERLEGD', 0, '2024-12-31 23:00:00+00', null, 'NL', 'BTW verlegd', 'REVERSE_CHARGE', now(), 'migration', now(), 'migration')
ON CONFLICT DO NOTHING;
