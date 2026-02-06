UPDATE catalog_items
SET vat_rate_id = (SELECT id FROM vat_rates WHERE vat_code = 'VERLEGD')
WHERE type = 'MATERIAL';