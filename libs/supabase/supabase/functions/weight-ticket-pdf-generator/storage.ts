import { createClient } from 'https://esm.sh/@supabase/supabase-js@2.39.0';
import * as postgres from 'https://deno.land/x/postgres@v0.17.0/mod.ts';

/**
 * Storage bucket name for weight ticket PDFs
 */
const STORAGE_BUCKET = 'weight-tickets';

/**
 * Type definition for storage options
 */
type StorageOptions = {
  contentType: string;
  upsert: boolean;
};

/**
 * Initialize Supabase client for storage operations
 * @returns Supabase client instance
 */
function getSupabaseClient() {
  const supabaseUrl = Deno.env.get('SUPABASE_URL') || '';
  const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') || '';
  return createClient(supabaseUrl, supabaseServiceKey);
}

// Initialize database connection pool for PDF URL updates
const databaseUrl = Deno.env.get('SUPABASE_DB_URL') || '';
const pool = new postgres.Pool(databaseUrl, 3, true);

/**
 * Generate a unique filename and storage path for the weight ticket PDF
 * Format: ddMMYYYY-weegbon-{weightticketid}.pdf
 * Path: /{weightticketid}/filename.pdf
 * @param ticketId - Weight ticket UUID
 * @param ticketNumber - Weight ticket number
 * @returns Object containing filename and storage path
 */
export function generateFileName(ticketId: string, ticketNumber: number): { filename: string; storagePath: string } {
  const now = new Date();
  const day = now.getDate().toString().padStart(2, '0');
  const month = (now.getMonth() + 1).toString().padStart(2, '0');
  const year = now.getFullYear();
  const filename = `${day}${month}${year}-weegbon-${ticketNumber}.pdf`;

  // Storage path: /{weightticketid}/filename.pdf
  const storagePath = `${ticketId}/${filename}`;

  return { filename, storagePath };
}

/**
 * Update the PDF URL in the weight_tickets table
 * @param ticketId - Weight ticket UUID
 * @param pdfUrl - Storage path for the PDF
 */
export async function updatePdfUrl(ticketId: string, pdfUrl: string): Promise<void> {
  let connection;
  try {
    connection = await pool.connect();
    await connection.queryObject`
      UPDATE weight_tickets
      SET pdf_url = ${pdfUrl}
      WHERE id = ${ticketId}::uuid
    `;
  } finally {
    if (connection) {
      connection.release();
    }
  }
}

/**
 * Upload a PDF file to Supabase storage
 * @param pdfBytes - Binary PDF content as Uint8Array
 * @param storagePath - Path/name for the file in storage
 * @returns Promise that resolves to the storage path when upload is complete
 * @throws Error if upload fails
 */
export async function uploadWeightTicketPdf(pdfBytes: Uint8Array, storagePath: string): Promise<string> {
  const supabase = getSupabaseClient();

  const options: StorageOptions = {
    contentType: 'application/pdf',
    upsert: true,
  };

  console.log(`Uploading PDF to storage: ${storagePath}`);

  const { error } = await supabase.storage.from(STORAGE_BUCKET).upload(storagePath, pdfBytes, options);

  if (error) {
    console.error(`Storage upload error:`, error);
    throw new Error(`Failed to upload PDF: ${error.message}`);
  }

  console.log(`PDF uploaded successfully to: ${storagePath}`);
  return storagePath;
}

/**
 * Download a weight ticket PDF from storage
 * @param storagePath - Path to the file in storage
 * @returns Promise resolving to the PDF blob
 * @throws Error if download fails
 */
export async function downloadWeightTicketPdf(storagePath: string): Promise<Blob> {
  const supabase = getSupabaseClient();

  console.log(`Downloading PDF from storage: ${storagePath}`);

  const { data, error } = await supabase.storage.from(STORAGE_BUCKET).download(storagePath);

  if (error || !data) {
    console.error(`Storage download error:`, error);
    throw new Error(`Failed to download PDF: ${error?.message || 'Unknown error'}`);
  }

  console.log(`PDF downloaded successfully from: ${storagePath}`);
  return data;
}
