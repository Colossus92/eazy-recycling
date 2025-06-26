import { PDFDocument, PageSizes } from 'npm:pdf-lib'
import { fetchTransportData, TransportData } from './db.ts'
import { drawBackgroundWaybill, drawData, drawSignatures } from './pdf.ts'
import { createClient } from 'npm:@supabase/supabase-js'
import { format } from 'npm:date-fns'

const supabase = createClient(
  Deno.env.get('SUPABASE_URL') ?? '',
  Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? '',
)


// Upload file using standard upload
async function uploadFile(file: Uint8Array, fileName: string) {
  console.log("Service role key: ", Deno.env.get('SUPABASE_SERVICE_ROLE_KEY'))
  const { error } = await supabase.storage.from('waybills').upload(fileName, file, {
    contentType: 'application/pdf',
    upsert: true
  })
  
  if (error) {
    throw error
  }
}
Deno.serve(async (req) => {
  try {
    const url = new URL(req.url);
    const pathParts = url.pathname.split('/').filter(Boolean);
    const transportId = pathParts[pathParts.length - 1];
    const partyType = url.searchParams.get('partyType');

    if (!partyType) {
      return new Response(JSON.stringify({ error: 'No party type specified' }), {
        status: 400,
        headers: { 'Content-Type': 'application/json' }
      });
    }

    const { data: transportData, response } = await fetchTransportData(transportId);

    if (response) return response;

    if (!transportData) {
      return new Response(JSON.stringify({ error: 'No transport data available' }), {
        status: 404,
        headers: { 'Content-Type': 'application/json' }
      });
    }

    const pdfBytes = await generatePdf(transportData);
    const timestamp = format(transportData.signatures.consignor_signed_at ?? new Date(), 'yyyy-MM-dd_HH-mm-ss');
    const fileName = `waybills/${transportData.transport.id}/waybill_${transportData.transport.display_number}_${partyType}_signed_${timestamp}.pdf`;
    
    await uploadFile(pdfBytes, fileName);

    return new Response(
      JSON.stringify({ success: true, message: 'PDF successfully generated and stored', fileName }),
      { 
        status: 201,
        headers: { "Content-Type": "application/json" } 
      },
    );
  } catch (err) {
    console.error(err);
    const errorMessage = err instanceof Error ? err.message : String(err);
    return new Response(errorMessage, { status: 500 });
  }
})

async function generatePdf(transportData: TransportData) {
  const pdfDoc = await PDFDocument.create();
  const page = pdfDoc.addPage(PageSizes.A4);

  await drawBackgroundWaybill(page, pdfDoc);
  drawData(page, transportData);
  await drawSignatures(page, pdfDoc, transportData);

  const pdfBytes = await pdfDoc.save();
  return pdfBytes;
}
