const RESEND_API_KEY = Deno.env.get('RESEND_API_KEY');

export async function sendEmail(
  to: string,
  subject: string,
  htmlBody: string,
  attachmentFileName: string,
  base64Data: string,
  bcc?: string
) {
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
