import { fetchWeightTicketData, pool } from './db.ts';
import { generateWeightTicketPdf } from './pdf.ts';
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2.39.0';

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
 * Extract ticket ID from request body or URL params
 * @param req - Request object
 * @returns Ticket ID
 */
async function extractTicketId(req: Request): Promise<string> {
  const url = new URL(req.url);
  const ticketIdFromParams = url.searchParams.get('ticketId');
  
  if (ticketIdFromParams) {
    return ticketIdFromParams;
  }

  try {
    const body = await req.json();
    return body.ticketId;
  } catch {
    throw new Error('No ticket ID provided');
  }
}

// Main server handler
Deno.serve(async (req) => {
  let connection;
  try {
    // Extract ticket ID from request
    const ticketId = await extractTicketId(req);
    console.log(`Generating PDF for weight ticket ${ticketId}`);

    // Fetch weight ticket data
    const { data: ticketData, response } = await fetchWeightTicketData(ticketId);
    
    if (response) {
      return response;
    }

    if (!ticketData) {
      return createApiResponse(404, {
        success: false,
        message: 'Weight ticket not found',
        error: 'No ticket data available'
      });
    }

    console.log(`[${ticketId}] Weight ticket found, generating PDF...`);

    // Generate PDF
    const pdfBytes = await generateWeightTicketPdf(ticketData);
    
    console.log(`[${ticketId}] PDF successfully generated`);

    // Initialize Supabase client
    const supabaseUrl = Deno.env.get('SUPABASE_URL') || '';
    const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') || '';
    const supabase = createClient(supabaseUrl, supabaseServiceKey);

    // Format filename with date: ddMMYYYY-weegbon-{weightticketid}.pdf
    const now = new Date();
    const day = now.getDate().toString().padStart(2, '0');
    const month = (now.getMonth() + 1).toString().padStart(2, '0');
    const year = now.getFullYear();
    const filename = `${day}${month}${year}-weegbon-${ticketData.weightTicket.id}.pdf`;
    
    // Storage path: /{weightticketid}/filename.pdf
    const storagePath = `${ticketData.weightTicket.id}/${filename}`;

    console.log(`[${ticketId}] Uploading PDF to storage: ${storagePath}`);

    // Upload PDF to Supabase storage
    const { error: uploadError } = await supabase.storage
      .from('weight-tickets')
      .upload(storagePath, pdfBytes, {
        contentType: 'application/pdf',
        upsert: true
      });

    if (uploadError) {
      console.error(`[${ticketId}] Storage upload error:`, uploadError);
      throw new Error(`Failed to upload PDF: ${uploadError.message}`);
    }

    console.log(`[${ticketId}] PDF uploaded successfully, updating database...`);

    // Update weight_tickets table with pdf_url
    connection = await pool.connect();
    await connection.queryObject`
      UPDATE weight_tickets
      SET pdf_url = ${storagePath}
      WHERE id = ${ticketData.weightTicket.id}
    `;

    console.log(`[${ticketId}] Database updated with PDF URL`);

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
  } finally {
    if (connection) {
      connection.release();
    }
  }
});
