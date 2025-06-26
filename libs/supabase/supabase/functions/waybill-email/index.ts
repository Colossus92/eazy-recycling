/**
 * Waybill Email Edge Function
 * This function mails the waybill PDF to the signee
 */

const RESEND_API_KEY = Deno.env.get('RESEND_API_KEY')

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
            path: 'https://resend.com/static/sample/invoice.pdf',
            filename: 'invoice.pdf',
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