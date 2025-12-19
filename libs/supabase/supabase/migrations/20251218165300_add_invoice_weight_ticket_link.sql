-- Add bidirectional link between invoices and weight tickets

-- Add source_weight_ticket_id to invoices table
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS source_weight_ticket_id BIGINT REFERENCES weight_tickets(id);

-- Add linked_invoice_id to weight_tickets table
ALTER TABLE weight_tickets ADD COLUMN IF NOT EXISTS linked_invoice_id BIGINT REFERENCES invoices(id);

-- Create indexes for efficient lookups
CREATE INDEX IF NOT EXISTS idx_invoices_source_weight_ticket_id ON invoices(source_weight_ticket_id);
CREATE INDEX IF NOT EXISTS idx_weight_tickets_linked_invoice_id ON weight_tickets(linked_invoice_id);
