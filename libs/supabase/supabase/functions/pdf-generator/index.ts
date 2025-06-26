import { PDFDocument, PageSizes } from 'npm:pdf-lib';
import { fetchTransportData, TransportData } from './db.ts';
import { drawBackgroundWaybill, drawData, drawSignatures } from './pdf.ts';
import { generateFileName, uploadFile } from './storage.ts';

// Type definitions
type ApiResponse = {
  success: boolean;
  message: string;
  fileName?: string;
  error?: string;
};

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
