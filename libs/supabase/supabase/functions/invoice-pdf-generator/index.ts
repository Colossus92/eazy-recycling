import { createClient } from 'https://esm.sh/@supabase/supabase-js@2';
import { generateInvoicePdf } from './pdf.ts';
import { InvoiceData } from './types.ts';

// Type definitions
type ApiResponse = {
  success: boolean;
  message: string;
  storagePath?: string;
  error?: string;
};

/**
 * Create a standardized API response
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
 * Upload PDF to Supabase Storage
 */
async function uploadToStorage(pdfBytes: Uint8Array, companyCode: string, invoiceNumber: string): Promise<string> {
  const supabaseUrl = Deno.env.get('SUPABASE_URL');
  const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY');

  if (!supabaseUrl || !supabaseServiceKey) {
    throw new Error('Missing Supabase environment variables');
  }

  const supabase = createClient(supabaseUrl, supabaseServiceKey);

  const storagePath = `${companyCode}/invoice-${invoiceNumber}.pdf`;

  const { error } = await supabase.storage
    .from('invoices')
    .upload(storagePath, pdfBytes, {
      contentType: 'application/pdf',
      upsert: true,
    });

  if (error) {
    throw new Error(`Failed to upload PDF to storage: ${error.message}`);
  }

  return storagePath;
}


// Main server handler
Deno.serve(async (req) => {
  try {
    console.log('Invoice PDF generation requested');
    let invoiceData: InvoiceData;

    if (req.method !== 'POST') {
      return createApiResponse(405, {
        success: false,
        message: 'Method not allowed. Use POST with invoice data in body, or GET with ?test=1|2|3 for test data.',
      });
    }

    const contentType = req.headers.get('content-type');
    if (!contentType?.includes('application/json')) {
      return createApiResponse(400, {
        success: false,
        message: 'Content-Type must be application/json',
      });
    }

    try {
      invoiceData = await req.json() as InvoiceData;
    } catch {
      return createApiResponse(400, {
        success: false,
        message: 'Invalid JSON in request body',
      });
    }

    // Basic validation
    if (!invoiceData.invoiceNumber || !invoiceData.invoiceType) {
      return createApiResponse(400, {
        success: false,
        message: 'Missing required fields: invoiceNumber and invoiceType are required',
      });
    }

    if (!invoiceData.lines || !Array.isArray(invoiceData.lines) || invoiceData.lines.length === 0) {
      return createApiResponse(400, {
        success: false,
        message: 'Missing required field: lines must be a non-empty array',
      });
    }


    console.log(`Generating PDF for invoice ${invoiceData.invoiceNumber}`);

    // Generate PDF
    const pdfBytes = await generateInvoicePdf(invoiceData);

    console.log(`PDF successfully generated for invoice ${invoiceData.invoiceNumber}`);

    // Upload to Supabase Storage
    const storagePath = await uploadToStorage(pdfBytes, invoiceData.companyCode, invoiceData.invoiceNumber);

    console.log(`PDF uploaded to storage: ${storagePath}`);

    return createApiResponse(200, {
      success: true,
      message: `Invoice PDF generated and stored successfully`,
      storagePath,
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
