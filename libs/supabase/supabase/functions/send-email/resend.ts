const RESEND_API_KEY = Deno.env.get('RESEND_API_KEY');

/**
 * Generates an idempotency key by hashing invoiceId + email address
 * This ensures the same invoice is only sent once to the same email address
 * Uses SHA-256 to obfuscate the email address for privacy
 */
async function generateIdempotencyKey(invoiceId: string, email: string): Promise<string> {
  const data = `${invoiceId}:${email}`;
  const encoder = new TextEncoder();
  const dataBuffer = encoder.encode(data);
  const hashBuffer = await crypto.subtle.digest('SHA-256', dataBuffer);
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  const hashHex = hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
  return `invoice/${hashHex}`;
}

export async function sendEmail(
  to: string,
  subject: string,
  htmlBody: string,
  attachmentFileName: string,
  base64Data: string,
  entityId: string,
  bcc?: string
) {
  const idempotencyKey = await generateIdempotencyKey(entityId, to);
  console.log('Generated idempotency key for invoice:', entityId);
  const emailPayload: {
    from: string;
    to: string;
    subject: string;
    html: string;
    attachments: Array<{ content: string; filename: string }>;
    bcc?: string;
  } = {
    from: 'noreply@eazysoftware.nl',
    to: to,
    subject: subject,
    html: htmlBody,
    attachments: [
      {
        content: base64Data,
        filename: attachmentFileName,
      },
    ],
  };

  if (bcc) {
    emailPayload.bcc = bcc;
  }

  const res = await fetch('https://api.resend.com/emails', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${RESEND_API_KEY}`,
      'Idempotency-Key': idempotencyKey,
    },
    body: JSON.stringify(emailPayload),
  });

  if (!res.ok) {
    const errorText = await res.text();
    console.error(`Resend API error (${res.status}):`, errorText);
    throw new Error(`Failed to send email: ${res.status} - ${errorText.substring(0, 200)}`);
  }

  const data = await res.json();
  console.log("Resend response:", data);
  return data;
}
