import { PDFDocument, PageSizes } from 'npm:pdf-lib';
import { fetchTransportData, TransportData } from './db.ts';
import { drawBackgroundWaybill, drawData, drawSignatures } from './pdf.ts';
import { generateFileName, uploadFile } from './storage.ts';
import { sendToQueue } from "./queue.ts";

// Type definitions
type ApiResponse = {
  success: boolean;
  message: string;
  fileName?: string;
  error?: string;
};

export type SigneeInfo = {
  type: string;
  signature?: string;
  signedAt?: Date | string;
  email?: string;
};

/**
 * Extract signee information based on party type
 * @param transportData - The transport data containing signatures
 * @param partyType - The type of party (consignor, consignee, etc.)
 * @returns Object containing signature, signed timestamp and email
 */
export function getSigneeInfo(partyType: string, transportData?: TransportData): SigneeInfo {
  // partyType is now passed directly as a parameter
  console.log('partyType', partyType);
  switch (partyType) {
    case 'consignor':
      return {
        type: 'consignor',
        signature: transportData?.signatures.consignor_signature,
        signedAt: transportData?.signatures.consignor_signed_at,
        email: transportData?.signatures.consignor_email
      };
    case 'consignee':
      return {
        type: 'consignee',
        signature: transportData?.signatures.consignee_signature,
        signedAt: transportData?.signatures.consignee_signed_at,
        email: transportData?.signatures.consignee_email
      };
    case 'pickup':
      return {
        type: 'pickup',
        signature: transportData?.signatures.pickup_signature,
        signedAt: transportData?.signatures.pickup_signed_at,
        email: transportData?.signatures.pickup_email
      };
    case 'carrier':
      return {
        type: 'carrier',
        signature: transportData?.signatures.carrier_signature,
        signedAt: transportData?.signatures.carrier_signed_at,
        email: transportData?.signatures.carrier_email
      };
    default:
      throw new Error('Invalid party type');
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
 * Extract transport ID and party type from request body
 * @param req - Request object
 * @returns Object containing transportId and partyType
 */
async function extractRequestData(req: Request): Promise<{ transportId: string, partyType: string }> {
  const body = await req.json();
  return {
    transportId: body.transportId,
    partyType: body.partyType
  };
}

// Main server handler
Deno.serve(async (req) => {
  try {
    // Extract data from request body instead of URL
    const { transportId, partyType } = await extractRequestData(req);
    
    // Fetch transport data
    const { data: transportData, response } = await fetchTransportData(transportId);
    if (response) return response;

    const signeeInfo = getSigneeInfo(partyType, transportData);

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
    const fileName = generateFileName(transportData, signeeInfo);
    await uploadFile(pdfBytes, fileName);
    await sendToQueue(signeeInfo, fileName);

    // Return success response with signee info
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
