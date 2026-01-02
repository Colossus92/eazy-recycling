import { createClient } from 'npm:@supabase/supabase-js@2';

const supabase = createClient(
  Deno.env.get('SUPABASE_URL') ?? '',
  Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
);

/**
 * Download a file from Supabase Storage
 * @param bucket - Storage bucket name
 * @param filePath - Path to the file in storage
 * @returns Base64 encoded file content
 */
export async function downloadFileFromStorage(bucket: string, filePath: string): Promise<{ base64Data: string }> {
  console.log(`Downloading file from bucket: ${bucket}, path: ${filePath}`);

  const { data, error } = await supabase
    .storage
    .from(bucket)
    .download(filePath);

  if (error) {
    console.error('Storage download error:', error);
    throw new Error(`Failed to download file: ${error.message}`);
  }

  if (!data) {
    throw new Error('No data received from storage');
  }

  const arrayBuffer = await data.arrayBuffer();

  let binary = '';
  const bytes = new Uint8Array(arrayBuffer);
  const len = bytes.byteLength;

  const chunkSize = 1024;
  for (let i = 0; i < len; i += chunkSize) {
    const chunk = bytes.slice(i, Math.min(i + chunkSize, len));
    const binaryChunk = Array.from(chunk)
      .map(byte => String.fromCharCode(byte))
      .join('');
    binary += binaryChunk;
  }

  const base64Data = btoa(binary);

  console.log(`File encoded successfully, size: ${base64Data.length} characters`);

  return { base64Data };
}
