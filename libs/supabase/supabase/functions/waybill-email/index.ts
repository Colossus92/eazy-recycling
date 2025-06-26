/**
 * Waybill Email Edge Function
 * This function mails the waybill PDF to the signee
 */

import { sendEmail } from "./resend.ts";
import { downloadFileFromStorage } from "./storage.ts";

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

async function sanitize(req: Request): Promise<{ body: EmailRequest | undefined, response: Response | undefined }> {
  const contentType = req.headers.get('content-type');
  let response: Response | undefined;

  if (!contentType || !contentType.includes('application/json')) {
    console.warn('Invalid Content-Type');
    response = createApiResponse(400, {
      success: false,
      message: 'Content-Type must be application/json',
    });
  }

  // Get request body as text first to debug
  const bodyText = await req.text();

  // Check if body is empty
  if (!bodyText || bodyText.trim() === '') {
    console.warn('Request body is empty');
    response = createApiResponse(400, {
      success: false,
      message: 'Request body is empty',
    });
  }

  // Parse the request body
  let body: EmailRequest | undefined;
  try {
    body = JSON.parse(bodyText) as EmailRequest;
  } catch (parseError) {
    console.warn('Invalid JSON in request body');
    response = createApiResponse(400, {
      success: false,
      message: 'Invalid JSON in request body',
      error: String(parseError),
    });
  }

  // Validate required fields
  if (!body || !body.email || !body.fileName) {
    console.warn('Missing required fields: email and fileName are required');
    response = createApiResponse(400, {
      success: false,
      message: 'Missing required fields: email and fileName are required',
    });
  }

  return {body, response};
}

// Main server handler
Deno.serve(async (req) => {
  try {
    // Check if request has content
    const {body, response} = await sanitize(req);

    if (response) {
      return response;
    }

    if (!body) {
      return createApiResponse(400, {
        success: false,
        message: 'Request body is empty',
      });
    }

    console.log('Received email request with body:', JSON.stringify(body, null, 2));

    const result = await downloadFileFromStorage(body.fileName);
    const base64Data: string = result.base64Data;

    if (!base64Data) {
      return createApiResponse(500, {
        success: false,
        message: 'Failed to download waybill PDF'
      });
    }

    await sendEmail(body.email, base64Data);

    // Return success response
    return createApiResponse(200, {
      success: true,
      message: 'Email send'
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