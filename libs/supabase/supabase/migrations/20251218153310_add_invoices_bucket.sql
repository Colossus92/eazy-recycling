-- Add pdf_url column to weight_tickets table
ALTER TABLE invoices ADD COLUMN pdf_url TEXT;

-- Create storage bucket for weight ticket PDFs
INSERT INTO storage.buckets (id, name, public)
VALUES ('invoices', 'invoices', false)
ON CONFLICT (id) DO NOTHING;

-- Set up RLS policies for invoices bucket
CREATE POLICY "Authenticated users can read invoices PDFs"
ON storage.objects FOR SELECT
TO authenticated
USING (bucket_id = 'invoices');

CREATE POLICY "Service role can insert invoices PDFs"
ON storage.objects FOR INSERT
TO service_role
WITH CHECK (bucket_id = 'invoices');

CREATE POLICY "Service role can update invoices PDFs"
ON storage.objects FOR UPDATE
TO service_role
USING (bucket_id = 'invoices');

CREATE POLICY "Service role can delete invoices PDFs"
ON storage.objects FOR DELETE
TO service_role
USING (bucket_id = 'invoices');
