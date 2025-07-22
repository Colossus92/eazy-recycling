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

/**
 * Fetch signature image from storage bucket
 * @param transportId - ID of the transport
 * @param partyType - Type of party (consignor, consignee, carrier, pickup)
 * @returns Promise resolving to signature data URL or null if not found
 */
export async function fetchSignatureFromBucket(transportId: string, partyType: string): Promise<string | null> {
  try {
    const signatureFileName = `signatures/${transportId}/${partyType}.png`;
    
    // Download the file
    const { data, error } = await supabase.storage
      .from(STORAGE_BUCKET)
      .download(signatureFileName);
    
    if (error || !data) {
      console.log(`Signature not found for ${partyType} in transport ${transportId}`);
      return null;
    }
    
    // Convert the blob to a base64 data URL
    const arrayBuffer = await data.arrayBuffer();
    const bytes = new Uint8Array(arrayBuffer);
    const base64 = btoa(String.fromCharCode(...bytes));
    
    return `data:image/png;base64,${base64}`;
  } catch (error) {
    console.error(`Error fetching signature for ${partyType}:`, error);
    return null;
  }
}

/**
 * Fetch all signatures for a transport
 * @param transportId - ID of the transport
 * @returns Promise resolving to an object containing all signatures
 */
export async function fetchAllSignatures(transportId: string): Promise<Record<string, string | undefined>> {
  const partyTypes = ['consignor', 'consignee', 'carrier', 'pickup'];
  
  const signatures: Record<string, string | undefined> = {};
  
  // Use Promise.all to fetch all signatures in parallel
  await Promise.all(
    partyTypes.map(async (partyType) => {
      const signature = await fetchSignatureFromBucket(transportId, partyType);
      signatures[`${partyType}_signature`] = signature || undefined;
    })
  );
  
  return signatures;
}