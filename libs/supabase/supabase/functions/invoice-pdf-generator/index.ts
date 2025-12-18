import { generateInvoicePdf } from './pdf.ts';
import { InvoiceData } from './types.ts';

// Type definitions
type ApiResponse = {
  success: boolean;
  message: string;
  error?: string;
};

/**
 * Create a standardized API response
 */
function createApiResponse(status: number, body: ApiResponse): Response {
  return new Response(
    JSON.stringify(body),
    {
      status,
      headers: { 'Content-Type': 'application/json' }
    }
  );
}

/**
 * Create a PDF response
 */
function createPdfResponse(pdfBytes: Uint8Array, filename: string): Response {
  return new Response(pdfBytes, {
    status: 200,
    headers: {
      'Content-Type': 'application/pdf',
      'Content-Disposition': `attachment; filename="${filename}"`,
    },
  });
}

/**
 * Hardcoded test data for development and testing
 */
function getTestInvoiceData(): InvoiceData {
  return {
    invoiceType: 'INKOOPFACTUUR',
    invoiceNumber: 'ER26000001',
    invoiceDate: '2024-12-18',
    paymentTermDays: 9,
    tenant: {
      name: 'WHD Kabel- en metaalrecycling',
      address: {
        street: 'Lotsweg',
        buildingNumber: '10',
        postalCode: '2635 NB',
        city: 'Den Hoorn',
      },
      phone: '+31 (0)85 902 4660',
      email: 'info@hetwestlandbv.nl',
      website: 'www.hetwestlandbv.nl',
      kvkNumber: '78083079',
      ibanNumber: 'NL91 ABNA 0417 1643 00',
      vatNumber: 'NL861258757B01',
    },
    customer: {
      name: 'Metaal & Recycling B.V.',
      address: {
        street: 'Industrieweg',
        buildingNumber: '123',
        postalCode: '3044 AB',
        city: 'Rotterdam',
        country: 'Nederland',
      },
      creditorNumber: '1000024',
      vatNumber: 'NL123456789B01',
    },
    lines: [
      {
        date: '2024-12-15',
        description: ['Koperkabel ongestript', 'Afvalstroomnummer: 170411000001'],
        orderNumber: '50231',
        quantity: 1250,
        unit: 'kg',
        vatPercentage: 21,
        pricePerUnit: 5.85,
        totalAmount: 7312.50, // 1250 * 5.85
      },
      {
        date: '2024-12-15',
        description: ['Aluminium gemengd', 'Afvalstroomnummer: 170402000002'],
        orderNumber: '50231',
        quantity: 850,
        unit: 'kg',
        vatPercentage: 21,
        pricePerUnit: 1.45,
        totalAmount: 1232.50, // 850 * 1.45
      },
      {
        date: '2024-12-16',
        description: ['Zink platen'],
        orderNumber: '50231',
        quantity: 500,
        unit: 'kg',
        vatPercentage: 9,
        pricePerUnit: 2.50,
        totalAmount: 1250.00, // 500 * 2.50, 9% BTW
      },
      {
        date: '2024-12-16',
        description: ['Roestvrij staal 304', 'Afvalstroomnummer: 170405000003'],
        orderNumber: '50231',
        quantity: 420,
        unit: 'kg',
        vatPercentage: 21,
        pricePerUnit: 1.85,
        totalAmount: 777.00, // 420 * 1.85
      },
      {
        date: '2024-12-17',
        description: ['Transport kosten'],
        orderNumber: '50231',
        quantity: 1,
        unit: 'st',
        vatPercentage: 'G',
        pricePerUnit: 150.00,
        totalAmount: 150.00, // No VAT (G)
      },
      {
        date: '2024-12-17',
        description: ['Messing gemengd', 'Afvalstroomnummer: 170401000004'],
        orderNumber: '50231',
        quantity: 180,
        unit: 'kg',
        vatPercentage: 21,
        pricePerUnit: 4.20,
        totalAmount: 756.00, // 180 * 4.20
      },
      {
        date: '2024-12-18',
        description: ['Industriële schroot partij', 'Afvalstroomnummer: 170407000005'],
        orderNumber: '50231',
        quantity: 1000,
        unit: 'ton',
        vatPercentage: 21,
        pricePerUnit: 1250.00,
        totalAmount: 1250000.00, // 1000 * 1250 = 1.25M
      },
      {
        date: '2024-12-17',
        description: ['Lood accu\'s', 'Afvalstroomnummer: 160601000006'],
        orderNumber: '50231',
        quantity: 320,
        unit: 'kg',
        vatPercentage: 21,
        pricePerUnit: 0.95,
        totalAmount: 304.00, // 320 * 0.95
      },
      {
        date: '2024-12-18',
        description: ['Koperkabel ongestript', 'Afvalstroomnummer: 170411000007'],
        orderNumber: '50232',
        quantity: 750,
        unit: 'kg',
        vatPercentage: 21,
        pricePerUnit: 5.85,
        totalAmount: 4387.50, // 750 * 5.85 - duplicate material
      },
    ],
    // Material totals (aggregated by material type)
    materialTotals: [
      { material: 'Koperkabel ongestript', totalWeight: 2000, unit: 'kg', totalAmount: 11700.00 }, // 1250 + 750 = 2000 kg
      { material: 'Aluminium gemengd', totalWeight: 850, unit: 'kg', totalAmount: 1232.50 },
      { material: 'Zink platen', totalWeight: 500, unit: 'kg', totalAmount: 1250.00 },
      { material: 'Roestvrij staal 304', totalWeight: 420, unit: 'kg', totalAmount: 777.00 },
      { material: 'Messing gemengd', totalWeight: 180, unit: 'kg', totalAmount: 756.00 },
      { material: 'Industriële schroot partij', totalWeight: 1000, unit: 'ton', totalAmount: 1250000.00 },
      { material: 'Lood accu\'s', totalWeight: 320, unit: 'kg', totalAmount: 304.00 },
      { material: 'Transport kosten', totalWeight: 1, unit: 'st', totalAmount: 150.00 },
    ],
    // Totals calculation:
    // Lines with 21% BTW: 7312.50 + 1232.50 + 777.00 + 756.00 + 1250000.00 + 304.00 + 4387.50 = 1264769.50
    // Lines with 9% BTW: 1250.00
    // Lines with G (no BTW): 150.00
    // Total excl VAT: 1264769.50 + 1250.00 + 150.00 = 1266169.50
    // VAT 21%: 1264769.50 * 0.21 = 265601.595
    // VAT 9%: 1250.00 * 0.09 = 112.50
    // Total VAT: 265601.60 + 112.50 = 265714.10
    // Total incl VAT: 1266169.50 + 265714.10 = 1531883.60
    totals: {
      totalExclVat: 1266169.50,
      vatAmount: 265714.10,
      totalInclVat: 1531883.60,
    },
  };
}

/**
 * Generate test data with many lines to create 2 pages
 */
function getTestInvoiceData2Pages(): InvoiceData {
  const baseData = getTestInvoiceData();
  
  // Generate additional lines to force 2 pages (about 20 lines total)
  const additionalLines: InvoiceData['lines'] = [];
  for (let i = 1; i <= 12; i++) {
    additionalLines.push({
      date: '2024-12-19',
      description: [`Extra materiaal ${i}`, `Afvalstroomnummer: 17040${String(i).padStart(7, '0')}`],
      orderNumber: '50233',
      quantity: 100 + i * 10,
      unit: 'kg',
      vatPercentage: 21,
      pricePerUnit: 1.50 + i * 0.10,
      totalAmount: (100 + i * 10) * (1.50 + i * 0.10),
    });
  }

  return {
    ...baseData,
    invoiceNumber: 'ER26000002',
    lines: [...baseData.lines, ...additionalLines],
    materialTotals: [
      ...baseData.materialTotals,
      ...additionalLines.map(l => ({
        material: l.description[0],
        totalWeight: l.quantity,
        unit: l.unit,
        totalAmount: l.totalAmount,
      })),
    ],
    totals: {
      totalExclVat: 1280000.00,
      vatAmount: 268800.00,
      totalInclVat: 1548800.00,
    },
  };
}

/**
 * Generate test data with many lines to create 3 pages
 */
function getTestInvoiceData3Pages(): InvoiceData {
  const baseData = getTestInvoiceData();
  
  // Generate additional lines to force 3 pages (about 50 lines total)
  const additionalLines: InvoiceData['lines'] = [];
  for (let i = 1; i <= 42; i++) {
    additionalLines.push({
      date: '2024-12-20',
      description: [`Bulk materiaal ${i}`, `Afvalstroomnummer: 17050${String(i).padStart(7, '0')}`],
      orderNumber: '50234',
      quantity: 50 + i * 5,
      unit: 'kg',
      vatPercentage: i % 3 === 0 ? 9 : 21,
      pricePerUnit: 2.00 + i * 0.05,
      totalAmount: (50 + i * 5) * (2.00 + i * 0.05),
    });
  }

  return {
    ...baseData,
    invoiceNumber: 'ER26000003',
    lines: [...baseData.lines, ...additionalLines],
    materialTotals: [
      ...baseData.materialTotals,
      ...additionalLines.map(l => ({
        material: l.description[0],
        totalWeight: l.quantity,
        unit: l.unit,
        totalAmount: l.totalAmount,
      })),
    ],
    totals: {
      totalExclVat: 1300000.00,
      vatAmount: 273000.00,
      totalInclVat: 1573000.00,
    },
  };
}

// Main server handler
Deno.serve(async (req) => {
  try {
    console.log('Invoice PDF generation requested');

    // Check URL for test mode
    const url = new URL(req.url);
    const testPages = url.searchParams.get('test');
    
    let invoiceData: InvoiceData;
    
    if (testPages) {
      // Test mode: use hardcoded test data
      console.log(`Test mode: generating ${testPages} page(s)`);
      if (testPages === '2') {
        invoiceData = getTestInvoiceData2Pages();
      } else if (testPages === '3') {
        invoiceData = getTestInvoiceData3Pages();
      } else {
        invoiceData = getTestInvoiceData();
      }
    } else {
      // Production mode: parse invoice data from request body
      if (req.method !== 'POST') {
        return createApiResponse(405, {
          success: false,
          message: 'Method not allowed. Use POST with invoice data in body, or GET with ?test=1|2|3 for test data.',
        });
      }

      const contentType = req.headers.get('content-type');
      if (!contentType?.includes('application/json')) {
        return createApiResponse(400, {
          success: false,
          message: 'Content-Type must be application/json',
        });
      }

      try {
        invoiceData = await req.json() as InvoiceData;
      } catch {
        return createApiResponse(400, {
          success: false,
          message: 'Invalid JSON in request body',
        });
      }

      // Basic validation
      if (!invoiceData.invoiceNumber || !invoiceData.invoiceType) {
        return createApiResponse(400, {
          success: false,
          message: 'Missing required fields: invoiceNumber and invoiceType are required',
        });
      }

      if (!invoiceData.lines || !Array.isArray(invoiceData.lines) || invoiceData.lines.length === 0) {
        return createApiResponse(400, {
          success: false,
          message: 'Missing required field: lines must be a non-empty array',
        });
      }
    }

    console.log(`Generating PDF for invoice ${invoiceData.invoiceNumber}`);

    // Generate PDF
    const pdfBytes = await generateInvoicePdf(invoiceData);
    
    console.log(`PDF successfully generated for invoice ${invoiceData.invoiceNumber}`);

    // Generate filename
    const filename = `${invoiceData.invoiceType.toLowerCase()}_${invoiceData.invoiceNumber}.pdf`;

    // Return PDF as downloadable file
    return createPdfResponse(pdfBytes, filename);

  } catch (err) {
    // Log error and return appropriate error response
    console.error('PDF generation error:', err);
    const errorMessage = err instanceof Error ? err.message : String(err);

    return createApiResponse(500, {
      success: false,
      message: 'Internal server error',
      error: errorMessage
    });
  }
});
