
import { format } from 'npm:date-fns';
import { TransportData } from './db.ts';
import { SigneeInfo } from './index.ts';
import { supabase } from './client.ts';

type StorageOptions = {
  contentType: string;
  upsert: boolean;
};

/**
 * Storage bucket name for waybill PDFs
 */
const STORAGE_BUCKET = 'waybills';

/**
 * Generate a unique filename for the PDF
 * @param transportData - Transport data
 * @param partyType - Type of party (consignor/consignee)
 * @returns Formatted filename with path
 */
export function generateFileName(transportData: TransportData, signeeInfo: SigneeInfo): string {
  // Use the signed timestamp or current date
  const dateToFormat = signeeInfo.signedAt || new Date();
  
  // Format the timestamp for the filename
  const timestamp = format(dateToFormat, 'yyyy-MM-dd_HH-mm-ss');
  
  return `${STORAGE_BUCKET}/${transportData.transport.id}/waybill_${transportData.transport.display_number}_${signeeInfo.type}_signed_${timestamp}.pdf`;
}

/**
 * Upload a PDF file to Supabase storage
 * @param file - Binary PDF content as Uint8Array
 * @param fileName - Path/name for the file in storage
 * @returns Promise that resolves when upload is complete
 * @throws Error if upload fails
 */
export async function uploadFile(file: Uint8Array, fileName: string): Promise<void> {
  const options: StorageOptions = {
    contentType: 'application/pdf',
    upsert: true
  };
  
  const { error } = await supabase.storage.from(STORAGE_BUCKET).upload(fileName, file, options);
  
  if (error) {
    console.error('Storage upload error:', error);
    throw new Error(`Failed to upload PDF: ${error.message}`);
  }
}