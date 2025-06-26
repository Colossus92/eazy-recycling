/**
 * Waybill Email Edge Function
 * This function receives email requests and logs the body content
 */

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

// Main server handler
Deno.serve(async (req) => {
  try {
    // Check if request has content
    const contentType = req.headers.get('content-type');
    if (!contentType || !contentType.includes('application/json')) {
      return createApiResponse(400, {
        success: false,
        message: 'Content-Type must be application/json',
      });
    }

    // Get request body as text first to debug
    const bodyText = await req.text();
    console.log('Raw request body:', bodyText);
    
    // Check if body is empty
    if (!bodyText || bodyText.trim() === '') {
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
      return createApiResponse(400, {
        success: false,
        message: 'Invalid JSON in request body',
        error: String(parseError),
      });
    }
    
    // Validate required fields
    if (!body.email || !body.fileName) {
      return createApiResponse(400, {
        success: false,
        message: 'Missing required fields: email and fileName are required',
      });
    }
    
    // Log the content of the body
    console.log('Received email request with body:', JSON.stringify(body, null, 2));
    
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