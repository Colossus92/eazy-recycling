/**
 * Generic Email Edge Function
 * Sends emails with PDF attachments using Resend
 */

import { sendEmail } from './resend.ts';
import { downloadFileFromStorage } from './storage.ts';

type ApiResponse = {
  success: boolean;
  message: string;
  error?: string;
};

type EmailRequest = {
  to: string;
  bcc?: string;
  subject: string;
  body: string;
  attachmentFileName: string;
  attachmentStorageBucket: string;
  attachmentStoragePath: string;
  invoiceId: string;
};

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

  const bodyText = await req.text();

  if (!bodyText || bodyText.trim() === '') {
    console.warn('Request body is empty');
    response = createApiResponse(400, {
      success: false,
      message: 'Request body is empty',
    });
  }

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

  if (!body || !body.to || !body.subject || !body.body || !body.attachmentFileName || !body.attachmentStorageBucket || !body.attachmentStoragePath || !body.invoiceId) {
    console.warn('Missing required fields');
    response = createApiResponse(400, {
      success: false,
      message: 'Missing required fields: to, subject, body, attachmentFileName, attachmentStorageBucket, attachmentStoragePath, invoiceId are required',
    });
  }

  return {body, response};
}

Deno.serve(async (req) => {
  try {
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

    console.log('Received email request:', {
      to: body.to,
      bcc: body.bcc,
      subject: body.subject,
      bucket: body.attachmentStorageBucket,
      path: body.attachmentStoragePath,
      invoiceId: body.invoiceId
    });

    const result = await downloadFileFromStorage(body.attachmentStorageBucket, body.attachmentStoragePath);
    const base64Data: string = result.base64Data;

    if (!base64Data) {
      return createApiResponse(500, {
        success: false,
        message: 'Failed to download PDF attachment'
      });
    }

    await sendEmail(
      body.to,
      body.subject,
      body.body,
      body.attachmentFileName,
      base64Data,
      body.invoiceId,
      body.bcc
    );

    return createApiResponse(200, {
      success: true,
      message: 'Email sent successfully'
    });
  } catch (err) {
    console.error('Email function error:', err);
    const errorMessage = err instanceof Error ? err.message : String(err);

    return createApiResponse(500, {
      success: false,
      message: 'Internal server error',
      error: errorMessage
    });
  }
});
