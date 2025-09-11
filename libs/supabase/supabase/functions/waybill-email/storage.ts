import { createClient } from "npm:@supabase/supabase-js@2";

const supabase = createClient(
    Deno.env.get('SUPABASE_URL') ?? '',
    Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? '',
);

// Storage bucket name for waybill PDFs
const STORAGE_BUCKET = 'waybills';


/**
 * Download a file from Supabase Storage
 * @param filePath - Path to the file in storage
 * @returns Base64 encoded file content
 */
export async function downloadFileFromStorage(filePath: string): Promise<{ base64Data: string }> {
    console.log(`Downloading file from bucket: ${STORAGE_BUCKET}, path: ${filePath}`);

    // Download the file
    const { data, error } = await supabase
        .storage
        .from(STORAGE_BUCKET)
        .download(filePath);

    if (error) {
        console.error('Storage download error:', error);
        throw new Error(`Failed to download file: ${error.message}`);
    }

    if (!data) {
        throw new Error('No data received from storage');
    }

    // First convert the blob to an ArrayBuffer
    const arrayBuffer = await data.arrayBuffer();

    // Convert to base64 using a safe chunking approach to avoid call stack issues
    // This is important for larger PDF files
    let binary = '';
    const bytes = new Uint8Array(arrayBuffer);
    const len = bytes.byteLength;

    // Process in chunks to avoid memory issues
    const chunkSize = 1024;
    for (let i = 0; i < len; i += chunkSize) {
        const chunk = bytes.slice(i, Math.min(i + chunkSize, len));
        const binaryChunk = Array.from(chunk)
            .map(byte => String.fromCharCode(byte))
            .join('');
        binary += binaryChunk;
    }

    // Use btoa to convert the binary string to base64
    const base64Data = btoa(binary);

    console.log(`File encoded successfully, size: ${base64Data.length} characters`);

    return { base64Data };
}