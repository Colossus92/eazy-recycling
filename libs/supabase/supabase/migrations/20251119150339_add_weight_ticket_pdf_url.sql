-- Add pdf_url column to weight_tickets table
ALTER TABLE weight_tickets ADD COLUMN pdf_url TEXT;

-- Create storage bucket for weight ticket PDFs
INSERT INTO storage.buckets (id, name, public)
VALUES ('weight-tickets', 'weight-tickets', false)
ON CONFLICT (id) DO NOTHING;

-- Set up RLS policies for weight-tickets bucket
CREATE POLICY "Authenticated users can read weight ticket PDFs"
ON storage.objects FOR SELECT
TO authenticated
USING (bucket_id = 'weight-tickets');

CREATE POLICY "Service role can insert weight ticket PDFs"
ON storage.objects FOR INSERT
TO service_role
WITH CHECK (bucket_id = 'weight-tickets');

CREATE POLICY "Service role can update weight ticket PDFs"
ON storage.objects FOR UPDATE
TO service_role
USING (bucket_id = 'weight-tickets');

CREATE POLICY "Service role can delete weight ticket PDFs"
ON storage.objects FOR DELETE
TO service_role
USING (bucket_id = 'weight-tickets');
