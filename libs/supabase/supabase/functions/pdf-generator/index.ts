import { PDFDocument, PageSizes } from 'npm:pdf-lib';
import { fetchTransportData, TransportData } from './db.ts';
import { drawBackgroundWaybill, drawData, drawSignatures } from './pdf.ts';
import { createClient } from 'npm:@supabase/supabase-js';
import { format } from 'npm:date-fns';

// Type definitions
type ApiResponse = {
  success: boolean;
  message: string;
  fileName?: string;
  error?: string;
};

type StorageOptions = {
  contentType: string;
  upsert: boolean;
};

/**
 * Initialize Supabase client with service role for privileged operations
 */
const supabase = createClient(
  Deno.env.get('SUPABASE_URL') ?? '',
  Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? '',
);

/**
 * Storage bucket name for waybill PDFs
 */
const STORAGE_BUCKET = 'waybills';

/**
 * Upload a PDF file to Supabase storage
 * @param file - Binary PDF content as Uint8Array
 * @param fileName - Path/name for the file in storage
 * @returns Promise that resolves when upload is complete
 * @throws Error if upload fails
 */
async function uploadFile(file: Uint8Array, fileName: string): Promise<void> {
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
 * Generate a PDF document from transport data
 * @param transportData - Data to be included in the PDF
 * @returns Promise resolving to PDF binary content
 */
async function generatePdf(transportData: TransportData): Promise<Uint8Array> {
  const pdfDoc = await PDFDocument.create();
  const page = pdfDoc.addPage(PageSizes.A4);

  await drawBackgroundWaybill(page, pdfDoc);
  drawData(page, transportData);
  await drawSignatures(page, pdfDoc, transportData);

  return await pdfDoc.save();
}

/**
 * Create a standardized API response
 * @param status - HTTP status code
 * @param body - Response body object
 * @returns Response object
 */
function createApiResponse(status: number, body: ApiResponse): Response {
  return new Response(
    JSON.stringify(body),
    { 
      status,
      headers: { "Content-Type": "application/json" } 
    }
  );
}

/**
 * Extract transport ID from request URL
 * @param url - Request URL
 * @returns Transport ID from the URL path
 */
function extractTransportId(url: URL): string {
  const pathParts = url.pathname.split('/').filter(Boolean);
  return pathParts[pathParts.length - 1];
}

/**
 * Generate a unique filename for the PDF
 * @param transportData - Transport data
 * @param partyType - Type of party (consignor/consignee)
 * @returns Formatted filename with path
 */
function generateFileName(transportData: TransportData, partyType: string): string {
  const timestamp = format(
    partyType === 'carrier' ? (transportData.signatures.carrier_signed_at ?? new Date()) :
    partyType === 'consignee' ? (transportData.signatures.consignee_signed_at ?? new Date()) :
    partyType === 'consignor' ? (transportData.signatures.consignor_signed_at ?? new Date()) :
    partyType === 'pickup' ? (transportData.signatures.pickup_signed_at ?? new Date()) :
    new Date(),
    'yyyy-MM-dd_HH-mm-ss'
  );
  
  return `${STORAGE_BUCKET}/${transportData.transport.id}/waybill_${transportData.transport.display_number}_${partyType}_signed_${timestamp}.pdf`;
}

// Main server handler
Deno.serve(async (req) => {
  try {
    const url = new URL(req.url);
    const transportId = extractTransportId(url);
    const partyType = url.searchParams.get('partyType');

    // Validate required parameters
    if (!partyType) {
      return createApiResponse(400, { 
        success: false, 
        message: 'Missing required parameter', 
        error: 'No party type specified' 
      });
    }

    // Fetch transport data
    const { data: transportData, response } = await fetchTransportData(transportId);

    // Return early if fetchTransportData returned a response
    if (response) return response;

    // Check if transport data exists
    if (!transportData) {
      return createApiResponse(404, { 
        success: false, 
        message: 'Resource not found', 
        error: 'No transport data available' 
      });
    }

    // Generate PDF and upload to storage
    const pdfBytes = await generatePdf(transportData);
    const fileName = generateFileName(transportData, partyType);
    await uploadFile(pdfBytes, fileName);

    // Return success response
    return createApiResponse(201, { 
      success: true, 
      message: 'PDF successfully generated and stored', 
      fileName 
    });
  } catch (err) {
    // Log error and return appropriate error response
    console.error('PDF generation error:', err);
    const errorMessage = err instanceof Error ? err.message : String(err);
    
    return createApiResponse(500, { 
      success: false, 
      message: 'Internal server error', 
      error: errorMessage 
    });
  }
});
