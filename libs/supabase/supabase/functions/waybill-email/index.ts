/**
 * Waybill Email Edge Function
 * This function mails the waybill PDF to the signee
 */

import { createClient } from "npm:@supabase/supabase-js@2";

// Initialize environment variables
const RESEND_API_KEY = Deno.env.get('RESEND_API_KEY');

// Initialize Supabase client with service role for privileged operations
const supabase = createClient(
  Deno.env.get('SUPABASE_URL') ?? '',
  Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? '',
);

// Storage bucket name for waybill PDFs
const STORAGE_BUCKET = 'waybills';

// Type definitions
type ApiResponse = {
  success: boolean;
  message: string;
  error?: string;
};

type EmailRequest = {
  email: string;
  fileName: string;
};

/**
 * Download a file from Supabase Storage
 * @param filePath - Path to the file in storage
 * @returns Base64 encoded file content
 */
async function downloadFileFromStorage(filePath: string): Promise<{ base64Data: string }> {  
  console.log(`Downloading file from bucket: ${STORAGE_BUCKET}, path: ${filePath}`);
  
  // Download the file
  const { data, error } = await supabase
    .storage
    .from(STORAGE_BUCKET)
    .download(filePath);
  
  if (error) {
    console.error('Storage download error:', error);
    throw new Error(`Failed to download file: ${error.message}`);
  }
  
  if (!data) {
    throw new Error('No data received from storage');
  }
  
  // For Resend API, we need to encode the file properly for email attachments
  // The Resend API expects base64 encoded data for attachments
  
  // In Deno, we can use the built-in btoa function for base64 encoding
  // But we need to handle binary data properly
  
  // First convert the blob to an ArrayBuffer
  const arrayBuffer = await data.arrayBuffer();
  
  // Convert to base64 using a safe chunking approach to avoid call stack issues
  // This is important for larger PDF files
  let binary = '';
  const bytes = new Uint8Array(arrayBuffer);
  const len = bytes.byteLength;
  
  // Process in chunks to avoid memory issues
  const chunkSize = 1024;
  for (let i = 0; i < len; i += chunkSize) {
    const chunk = bytes.slice(i, Math.min(i + chunkSize, len));
    const binaryChunk = Array.from(chunk)
      .map(byte => String.fromCharCode(byte))
      .join('');
    binary += binaryChunk;
  }
  
  // Use btoa to convert the binary string to base64
  const base64Data = btoa(binary);
  
  console.log(`File encoded successfully, size: ${base64Data.length} characters`);
  
  return { base64Data };
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

// Main server handler
Deno.serve(async (req) => {
  try {
    // Check if request has content
    const contentType = req.headers.get('content-type');
    if (!contentType || !contentType.includes('application/json')) {
      console.warn('Invalid Content-Type');
      return createApiResponse(400, {
        success: false,
        message: 'Content-Type must be application/json',
      });
    }

    // Get request body as text first to debug
    const bodyText = await req.text();
    
    // Check if body is empty
    if (!bodyText || bodyText.trim() === '') {
      console.warn('Request body is empty');
      return createApiResponse(400, {
        success: false,
        message: 'Request body is empty',
      });
    }
    
    // Parse the request body
    let body: EmailRequest;
    try {
      body = JSON.parse(bodyText) as EmailRequest;
    } catch (parseError) {
      console.warn('Invalid JSON in request body');
      return createApiResponse(400, {
        success: false,
        message: 'Invalid JSON in request body',
        error: String(parseError),
      });
    }
    
    // Validate required fields
    if (!body.email || !body.fileName) {
      console.warn('Missing required fields: email and fileName are required');
      return createApiResponse(400, {
        success: false,
        message: 'Missing required fields: email and fileName are required',
      });
    }
    
    console.log('Received email request with body:', JSON.stringify(body, null, 2));
    
    // Create a nicely formatted HTML email in Dutch
    const emailHtml = `
      <!DOCTYPE html>
      <html>
      <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Begeleidingsbrief</title>
        <style>
          body {
            font-family: Arial, sans-serif;
            line-height: 1.6;
            color: #333;
            max-width: 600px;
            margin: 0 auto;
          }
          .content {
            padding: 20px;
          }
          .footer {
            background-color: #f1f1f1;
            padding: 10px 20px;
            font-size: 12px;
            text-align: center;
          }
          .button {
            background-color: #4CAF50;
            color: white;
            padding: 10px 15px;
            text-decoration: none;
            border-radius: 4px;
            display: inline-block;
            margin-top: 10px;
          }
        </style>
      </head>
      <body>
        <div class="content">
          <h2>Begeleidingsbrief</h2>
          <p>Beste,</p>
          <p>Hierbij ontvangt u de begeleidingsbrief voor uw afvaltransport.</p>
          <p>Met vriendelijke groet,</p>
          <p>
            <strong>WHD Metaalrecycling</strong><br>
          </p>
        </div>
        <div class="footer">
          <p>Dit is een automatisch gegenereerd bericht. Reageren op deze e-mail is niet mogelijk.</p>
        </div>
      </body>
      </html>
    `;

    // Download the PDF file from storage
    console.log(`Attempting to download file: ${body.fileName}`);
    let base64Data: string | undefined;
    
    try {
      // Download the file from Supabase Storage
      const result = await downloadFileFromStorage(body.fileName);
      base64Data = result.base64Data;
           
      console.log(`Successfully downloaded file: ${body.fileName}`);
    } catch (downloadError) {
      console.error('Error downloading file:', downloadError);
    }
    
    if (!base64Data) {
      return createApiResponse(500, { 
        success: false, 
        message: 'Failed to download waybill PDF' 
      });
    }

    // Send the email with the waybill attached
    const res = await fetch('https://api.resend.com/emails', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${RESEND_API_KEY}`,
      },
      body: JSON.stringify({
        from: 'onboarding@resend.dev',
        to: body.email,
        subject: `Begeleidingsbrief - WHD Metaalrecycling`,
        html: emailHtml,
        attachments: [
          {
            content: base64Data,
            filename: 'begeleidingsbrief.pdf',
          },
        ],
      }),
    })
    const data = await res.json()

    console.log("Response:", data)
    
    // Return success response
    return createApiResponse(200, { 
      success: true, 
      message: 'Email request received and logged' 
    });
  } catch (err) {
    // Log error and return appropriate error response
    console.error('Email function error:', err);
    const errorMessage = err instanceof Error ? err.message : String(err);
    
    return createApiResponse(500, { 
      success: false, 
      message: 'Internal server error', 
      error: errorMessage 
    });
  }
});