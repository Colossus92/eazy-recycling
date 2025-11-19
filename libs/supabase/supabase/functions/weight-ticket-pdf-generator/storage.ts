import { createClient } from 'https://esm.sh/@supabase/supabase-js@2.39.0';
import { WeightTicketData } from './db.ts';

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

/**
 * Generate a unique filename and storage path for the weight ticket PDF
 * Format: ddMMYYYY-weegbon-{weightticketid}.pdf
 * Path: /{weightticketid}/filename.pdf
 * @param ticketData - Weight ticket data
 * @returns Object containing filename and storage path
 */
export function generateFileName(ticketData: WeightTicketData): { filename: string; storagePath: string } {
  const now = new Date();
  const day = now.getDate().toString().padStart(2, '0');
  const month = (now.getMonth() + 1).toString().padStart(2, '0');
  const year = now.getFullYear();
  const filename = `${day}${month}${year}-weegbon-${ticketData.weightTicket.id}.pdf`;

  // Storage path: /{weightticketid}/filename.pdf
  const storagePath = `${ticketData.weightTicket.id}/${filename}`;

  return { filename, storagePath };
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
