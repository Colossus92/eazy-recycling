-- Create bucket only if it doesn't exist
INSERT INTO storage.buckets ("id", "name", "owner", "created_at", "updated_at", "public", "avif_autodetection", "file_size_limit", "allowed_mime_types", "owner_id", "type") 
SELECT 'waybills', 'waybills', null, '2025-06-26 13:05:10.630011+00', '2025-06-26 13:05:10.630011+00', 'false', 'false', null, null, null, 'STANDARD'
WHERE NOT EXISTS (SELECT 1 FROM storage.buckets WHERE id = 'waybills');

-- Create policies only if they don't exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'Authenticated can list and upload bd9ltp_0' AND tablename = 'objects' AND schemaname = 'storage') THEN
        CREATE POLICY "Authenticated can list and upload bd9ltp_0" ON storage.objects FOR SELECT TO authenticated USING (bucket_id = 'waybills');
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'Authenticated can list and upload bd9ltp_1' AND tablename = 'objects' AND schemaname = 'storage') THEN
        CREATE POLICY "Authenticated can list and upload bd9ltp_1" ON storage.objects FOR INSERT TO authenticated WITH CHECK (bucket_id = 'waybills');
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'Authenticated can list and upload bd9ltp_2' AND tablename = 'objects' AND schemaname = 'storage') THEN
        CREATE POLICY "Authenticated can list and upload bd9ltp_2" ON storage.objects FOR UPDATE TO authenticated USING (bucket_id = 'waybills');
    END IF;
END $$;