
// Initialize environment variables
const RESEND_API_KEY = Deno.env.get('RESEND_API_KEY');
const RESEND_FROM_EMAIL = Deno.env.get('RESEND_FROM_EMAIL');



export async function sendEmail(email: string, base64Data: string) {
    if (!RESEND_FROM_EMAIL) {
        throw new Error('RESEND_FROM_EMAIL environment variable is not set');
    }
    
    const res = await fetch('https://api.resend.com/emails', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${RESEND_API_KEY}`,
        },
        body: JSON.stringify({
            from: RESEND_FROM_EMAIL,
            to: email,
            subject: `Begeleidingsbrief - WHD Metaalrecycling`,
            html: emailHtml,
            attachments: [
                {
                    content: base64Data,
                    filename: 'begeleidingsbrief.pdf',
                },
            ],
        }),
    });

    // Check if response is OK before parsing JSON
    if (!res.ok) {
        const errorText = await res.text();
        console.error(`Resend API error (${res.status}):`, errorText);
        throw new Error(`Failed to send email: ${res.status} - ${errorText.substring(0, 200)}`);
    }

    const data = await res.json();
    console.log("Resend response:", data);
    return data;
}

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
