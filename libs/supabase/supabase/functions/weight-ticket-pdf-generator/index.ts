import { generateWeightTicketPdf, WeightTicketPdfData } from './pdf.ts';
import {
  generateFileName,
  updatePdfUrl,
  uploadWeightTicketPdf,
} from './storage.ts';

// Type definitions
type ApiResponse = {
  success: boolean;
  message: string;
  ticketId?: number;
  error?: string;
};

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
      headers: { 'Content-Type': 'application/json' }
    }
  );
}

/**
 * Create a PDF response
 * @param pdfBytes - PDF binary content
 * @param filename - PDF filename
 * @returns Response object
 */
function createPdfResponse(pdfBytes: Uint8Array, filename: string): Response {
  return new Response(pdfBytes, {
    status: 200,
    headers: {
      'Content-Type': 'application/pdf',
      'Content-Disposition': `attachment; filename="${filename}"`,
    },
  });
}

/**
 * Extract weight ticket PDF data from request body
 * @param req - Request object
 * @returns Weight ticket PDF data
 */
async function extractPdfData(req: Request): Promise<WeightTicketPdfData> {
  try {
    const body = await req.json();

    if (!body.weightTicket) {
      throw new Error('Missing weightTicket data in request body');
    }

    return body as WeightTicketPdfData;
  } catch (err) {
    if (err instanceof Error && err.message.includes('Missing')) {
      throw err;
    }
    throw new Error('Invalid request body - expected JSON with weight ticket data');
  }
}

// Main server handler
Deno.serve(async (req) => {
  try {
    // Extract weight ticket data from request body
    const ticketData = await extractPdfData(req);
    const ticketNumber = ticketData.weightTicket.number;

    console.log(`Generating PDF for weight ticket ${ticketNumber}`);

    // Generate PDF
    const pdfBytes = await generateWeightTicketPdf(ticketData);

    console.log(`[${ticketNumber}] PDF successfully generated`);

    // Generate filename and storage path
    const { filename, storagePath } = generateFileName(ticketData.weightTicket.id, ticketData.weightTicket.number);

    // Upload PDF to Supabase storage
    await uploadWeightTicketPdf(pdfBytes, storagePath);

    console.log(`[${ticketNumber}] PDF uploaded successfully, updating database...`);

    // Update weight_tickets table with pdf_url
    await updatePdfUrl(ticketData.weightTicket.id, storagePath);

    console.log(`[${ticketNumber}] Database updated with PDF URL`);

    // Return PDF as downloadable file
    return createPdfResponse(pdfBytes, filename);

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
