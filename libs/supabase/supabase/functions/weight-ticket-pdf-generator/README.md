# Weight Ticket PDF Generator

A Supabase Edge Function that generates PDF documents for weight tickets.

## Features

- Generates A4-sized PDF documents for weight tickets
- Displays company information (hardcoded) in the top right corner
- Includes a placeholder for SVG logo in the top left
- Retrieves weight ticket data from the database including:
  - Ticket information (ID, status, truck license plate, etc.)
  - Consignor party details
  - Carrier party details
  - Pickup location details
  - Delivery location details
  - Tarra weight information
  - Notes and reclamation information
  - Cancellation reason (if applicable)

## API Usage

### Request

**Method:** GET or POST

**Endpoint:** `https://your-project.supabase.co/functions/v1/weight-ticket-pdf-generator`

#### Query Parameters (GET)

```text
?ticketId=123
```

#### Request Body (POST)

```json
{
  "ticketId": "123"
}
```

### Response

The function returns a PDF file with the appropriate headers for download:

```text
Content-Type: application/pdf
Content-Disposition: attachment; filename="weight-ticket-{id}.pdf"
```

### Error Responses

#### 400 Bad Request

```json
{
  "success": false,
  "message": "Resource not found",
  "error": "Invalid ticket ID format"
}
```

#### 404 Not Found

```json
{
  "success": false,
  "message": "Weight ticket not found",
  "error": "No ticket data available"
}
```

#### 500 Internal Server Error

```json
{
  "success": false,
  "message": "Internal server error",
  "error": "Error details..."
}
```

## Database Schema

The function queries the following tables:

- `weight_tickets` - Main weight ticket data
- `companies` - Consignor and carrier party information
- `pickup_locations` - Pickup and delivery location information

## Local Development

```bash
cd libs/supabase/supabase/functions/weight-ticket-pdf-generator
deno task dev
```

## Deployment

Deploy this function to Supabase:

```bash
supabase functions deploy weight-ticket-pdf-generator
```

## Customization

### Company Address

The company address displayed in the top right corner can be customized by editing the `COMPANY_ADDRESS` constant in `pdf.ts`:

```typescript
const COMPANY_ADDRESS = {
  name: 'Your Company Name',
  street: 'Your Street Address',
  postalCode: 'Your Postal Code',
  city: 'Your City',
  country: 'Your Country',
  phone: 'Your Phone Number',
  email: 'Your Email'
};
```

### Logo

The function loads `logo.png` as a static file bundled with the Edge Function. The logo is configured in `supabase/config.toml` under the function's `static_files` array.

To update the logo:

1. Replace `logo.svg` with your SVG logo file
2. Convert it to PNG using: `rsvg-convert -w 480 -h 240 logo.svg -o logo.png`
3. Ensure `logo.png` is listed in the `static_files` configuration in `config.toml`
4. Deploy the function with `supabase functions deploy weight-ticket-pdf-generator`

The logo is loaded using `import.meta.url` for proper path resolution in Deno Deploy. If the logo cannot be loaded, a placeholder will be displayed instead.

## Dependencies

- `pdf-lib` - PDF generation library
- `postgres` - PostgreSQL client for Deno

## File Structure

```text
weight-ticket-pdf-generator/
├── README.md          # This file
├── index.ts           # Main entry point and request handler
├── db.ts              # Database queries and data fetching
├── pdf.ts             # PDF generation and drawing logic
├── deno.json          # Deno configuration
└── logo.png           # Company logo (PNG for PDF embedding)
```
