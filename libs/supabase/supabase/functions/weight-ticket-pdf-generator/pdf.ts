import type { PDFPage } from 'npm:pdf-lib';
import { PDFDocument, rgb, StandardFonts } from 'npm:pdf-lib';

/**
 * Type definitions for weight ticket PDF data received from backend
 */
export interface WeightTicketPdfData {
  weightTicket: {
    id: string;
    number: number;
    truckLicensePlate: string;
    reclamation?: string;
    direction: string;
    weightedAt?: string;
    createdAt: string;
    weging1: number;
    weging2: number;
    grossWeight: number;
    tarraWeight: number;
    nettoWeight: number;
    weightUnit: string;
  };
  lines: WeightTicketLine[];
  productLines?: WeightTicketProductLine[];
  consignorParty?: PartyData;
  carrierParty?: PartyData;
  pickupLocation?: LocationData;
  deliveryLocation?: LocationData;
}

export interface WeightTicketLine {
  wasteTypeName: string;
  weightValue: number;
  weightUnit: string;
}

export interface WeightTicketProductLine {
  productName: string;
  quantity: number;
  unit: string;
}

export interface PartyData {
  name?: string;
  streetName?: string;
  buildingNumber?: string;
  postalCode?: string;
  city?: string;
}

export interface LocationData {
  name?: string;
  streetName?: string;
  buildingNumber?: string;
  postalCode?: string;
  city?: string;
}

// Hardcoded company address for top right
const COMPANY_ADDRESS = {
    name: 'WHD Kabel- en metaalrecycling',
    street: 'Fultonstraat 4',
    postalCode: '2691 HA',
    city: ' \'s-GravenZande',
    phone: '+31 (0)85 902 4660',
    email: 'info@hetwestlandbv.nl',
    chamberOfCommerce: '78083079',
    vatNumber: 'NL 861258757 B01',
};

/**
 * Draw the company header with logo and address
 */
async function drawHeader(page: PDFPage, pdfDoc: PDFDocument) {
    const { width, height } = page.getSize();
    const font = await pdfDoc.embedFont(StandardFonts.Helvetica);
    const boldFont = await pdfDoc.embedFont(StandardFonts.HelveticaBold);

    // Draw logo in top left
    const logoX = 40;
    const logoY = height - 100;
    const logoWidth = 120;
    const logoHeight = 60;

    try {
        // Load the logo PNG file using import.meta.url for correct path resolution
        const logoUrl = new URL('./logo.png', import.meta.url);
        const logoBytes = await Deno.readFile(logoUrl);
        const logoImage = await pdfDoc.embedPng(logoBytes);

        page.drawImage(logoImage, {
            x: logoX,
            y: logoY,
            width: logoWidth,
            height: logoHeight,
        });
    } catch (error) {
        console.warn('Could not load logo, using placeholder:', error);
        // Fallback to placeholder if logo cannot be loaded
        page.drawRectangle({
            x: logoX,
            y: logoY,
            width: logoWidth,
            height: logoHeight,
            borderColor: rgb(0.7, 0.7, 0.7),
            borderWidth: 1,
        });

        page.drawText('LOGO', {
            x: logoX + 35,
            y: logoY + 25,
            size: 14,
            font: boldFont,
            color: rgb(0.7, 0.7, 0.7),
        });
    }

    // Draw company address in top right
    const rightX = width - 40;
    let addressY = height - 50;

    page.drawText(COMPANY_ADDRESS.name, {
        x: rightX - 150,
        y: addressY,
        size: 10,
        font: boldFont,
        color: rgb(0, 0, 0),
    });

    addressY -= 15;
    page.drawText(COMPANY_ADDRESS.street, {
        x: rightX - 150,
        y: addressY,
        size: 9,
        font,
        color: rgb(0, 0, 0),
    });

    addressY -= 15;
    page.drawText(`${COMPANY_ADDRESS.postalCode} ${COMPANY_ADDRESS.city}`, {
        x: rightX - 150,
        y: addressY,
        size: 9,
        font,
        color: rgb(0, 0, 0),
    });

    addressY -= 15;
    page.drawText(COMPANY_ADDRESS.phone, {
        x: rightX - 150,
        y: addressY,
        size: 9,
        font,
        color: rgb(0, 0, 0),
    });

    addressY -= 15;
    page.drawText(COMPANY_ADDRESS.email, {
        x: rightX - 150,
        y: addressY,
        size: 9,
        font,
        color: rgb(0, 0, 0),
    });

    addressY -= 15;
    page.drawText(`KvK: ${COMPANY_ADDRESS.chamberOfCommerce}`, {
        x: rightX - 150,
        y: addressY,
        size: 8,
        font,
        color: rgb(0.3, 0.3, 0.3),
    });

    addressY -= 12;
    page.drawText(`BTW: ${COMPANY_ADDRESS.vatNumber}`, {
        x: rightX - 150,
        y: addressY,
        size: 8,
        font,
        color: rgb(0.3, 0.3, 0.3),
    });
}

/**
 * Draw the title section
 */
async function drawTitle(page: PDFPage, pdfDoc: PDFDocument, ticketId: number) {
    const { width, height } = page.getSize();
    const titleFont = await pdfDoc.embedFont(StandardFonts.HelveticaBold);

    const titleY = height - 200;

    page.drawText('WEEGBON', {
        x: 40,
        y: titleY,
        size: 20,
        font: titleFont,
        color: rgb(0, 0, 0),
    });

    page.drawText(`#${ticketId}`, {
        x: 40,
        y: titleY - 25,
        size: 12,
        font: titleFont,
        color: rgb(0.3, 0.3, 0.3),
    });

    // Draw a horizontal line
    page.drawLine({
        start: { x: 40, y: titleY - 35 },
        end: { x: width - 40, y: titleY - 35 },
        thickness: 1,
        color: rgb(0.7, 0.7, 0.7),
    });
}

/**
 * Draw a section with label and value
 */
async function drawField(
    page: PDFPage,
    pdfDoc: PDFDocument,
    x: number,
    y: number,
    label: string,
    value: string,
    labelWidth = 150
) {
    const regularFont = await pdfDoc.embedFont(StandardFonts.Helvetica);
    const boldFont = await pdfDoc.embedFont(StandardFonts.HelveticaBold);

    // Draw label
    page.drawText(label, {
        x,
        y,
        size: 10,
        font: boldFont,
        color: rgb(0.3, 0.3, 0.3),
    });

    // Draw value
    page.drawText(value || '-', {
        x: x + labelWidth,
        y,
        size: 10,
        font: regularFont,
        color: rgb(0, 0, 0),
    });
}

/**
 * Draw a party section (company or location)
 */
async function drawPartySection(
    page: PDFPage,
    pdfDoc: PDFDocument,
    x: number,
    y: number,
    title: string,
    party?: PartyData | LocationData
) {
    const regularFont = await pdfDoc.embedFont(StandardFonts.Helvetica);
    const boldFont = await pdfDoc.embedFont(StandardFonts.HelveticaBold);

    // Draw title
    page.drawText(title, {
        x,
        y,
        size: 11,
        font: boldFont,
        color: rgb(0, 0, 0),
    });

    if (!party) {
        page.drawText('Not specified', {
            x,
            y: y - 15,
            size: 9,
            font: regularFont,
            color: rgb(0.5, 0.5, 0.5),
        });
        return;
    }

    let currentY = y - 15;

    // Draw name
    if (party.name) {
        page.drawText(party.name, {
            x,
            y: currentY,
            size: 9,
            font: regularFont,
            color: rgb(0, 0, 0),
        });
        currentY -= 12;
    }

    // Draw address
    if (party.streetName && party.buildingNumber) {
        page.drawText(`${party.streetName} ${party.buildingNumber}`, {
            x,
            y: currentY,
            size: 9,
            font: regularFont,
            color: rgb(0, 0, 0),
        });
        currentY -= 12;
    }

    // Draw postal code and city
    if (party.postalCode && party.city) {
        page.drawText(`${party.postalCode} ${party.city}`, {
            x,
            y: currentY,
            size: 9,
            font: regularFont,
            color: rgb(0, 0, 0),
        });
        currentY -= 12;
    }

}


/**
 * Draw weight ticket lines with totals using pre-calculated weights from backend
 */
async function drawLinesTable(
    page: PDFPage,
    pdfDoc: PDFDocument,
    x: number,
    y: number,
    lines: WeightTicketLine[],
    productLines: WeightTicketProductLine[],
    weging1: number,
    weging2: number,
    grossWeight: number,
    tarraWeight: number,
    nettoWeight: number,
    weightUnit: string
): Promise<number> {
    const regularFont = await pdfDoc.embedFont(StandardFonts.Helvetica);
    const boldFont = await pdfDoc.embedFont(StandardFonts.HelveticaBold);

    let currentY = y;
    
    // Use left half of page - more concise layout
    const weightValueX = x + 220;

    // Draw Weging 1
    page.drawText(`Weging 1:`, {
        x,
        y: currentY,
        size: 10,
        font: regularFont,
        color: rgb(0, 0, 0),
    });

    const weging1Text = `${weging1.toFixed(2)} ${weightUnit}`;
    const weging1Width = regularFont.widthOfTextAtSize(weging1Text, 10);
    page.drawText(weging1Text, {
        x: weightValueX - weging1Width,
        y: currentY,
        size: 10,
        font: regularFont,
        color: rgb(0, 0, 0),
    });

    currentY -= 15;

    // Draw Weging 2
    page.drawText(`Weging 2:`, {
        x,
        y: currentY,
        size: 10,
        font: regularFont,
        color: rgb(0, 0, 0),
    });

    const weging2Text = `${weging2.toFixed(2)} ${weightUnit}`;
    const weging2Width = regularFont.widthOfTextAtSize(weging2Text, 10);
    page.drawText(weging2Text, {
        x: weightValueX - weging2Width,
        y: currentY,
        size: 10,
        font: regularFont,
        color: rgb(0, 0, 0),
    });

    currentY -= 18;

    // Draw separator line
    page.drawLine({
        start: { x, y: currentY },
        end: { x: weightValueX, y: currentY },
        thickness: 0.5,
        color: rgb(0.7, 0.7, 0.7),
    });

    currentY -= 15;

    // Draw Bruto (gross weight)
    page.drawText(`Bruto:`, {
        x,
        y: currentY,
        size: 10,
        font: boldFont,
        color: rgb(0, 0, 0),
    });

    const brutoText = `${grossWeight.toFixed(2)} ${weightUnit}`;
    const brutoWidth = regularFont.widthOfTextAtSize(brutoText, 10);
    page.drawText(brutoText, {
        x: weightValueX - brutoWidth,
        y: currentY,
        size: 10,
        font: regularFont,
        color: rgb(0, 0, 0),
    });

    currentY -= 15;

    // Draw Emballage (instead of Tarra)
    const emballageText = tarraWeight > 0 ? `${tarraWeight.toFixed(2)} ${weightUnit}` : '-';
    page.drawText(`Emballage:`, {
        x,
        y: currentY,
        size: 10,
        font: regularFont,
        color: rgb(0, 0, 0),
    });

    const emballageWidth = regularFont.widthOfTextAtSize(emballageText, 10);
    page.drawText(emballageText, {
        x: weightValueX - emballageWidth,
        y: currentY,
        size: 10,
        font: regularFont,
        color: rgb(0, 0, 0),
    });

    currentY -= 18;

    // Draw separator line
    page.drawLine({
        start: { x, y: currentY },
        end: { x: weightValueX, y: currentY },
        thickness: 0.5,
        color: rgb(0.7, 0.7, 0.7),
    });

    currentY -= 15;

    // Draw Netto - same font size and weight as Bruto
    page.drawText(`Netto:`, {
        x,
        y: currentY,
        size: 10,
        font: boldFont,
        color: rgb(0, 0, 0),
    });

    const nettoText = `${nettoWeight.toFixed(2)} ${weightUnit}`;
    const nettoWidth = regularFont.widthOfTextAtSize(nettoText, 10);
    page.drawText(nettoText, {
        x: weightValueX - nettoWidth,
        y: currentY,
        size: 10,
        font: regularFont,
        color: rgb(0, 0, 0),
    });

    currentY -= 20;

    page.drawText('Uitsortering hoofdmaterialen:', {
        x,
        y: currentY,
        size: 11,
        font: boldFont,
        color: rgb(0, 0, 0),
    });
    currentY -= 13;
    
    // Draw each line (waste type and weight combined) - using left half
    // All weights are pre-calculated and formatted by backend
    for (const line of lines) {
        const weightText = `${line.weightValue.toFixed(2)} ${line.weightUnit || ''}`;

        page.drawText(line.wasteTypeName || 'Onbekend', {
            x,
            y: currentY,
            size: 9,
            font: regularFont,
            color: rgb(0, 0, 0),
        });

        const lineWeightWidth = regularFont.widthOfTextAtSize(weightText, 9);
        page.drawText(weightText, {
            x: weightValueX - lineWeightWidth,
            y: currentY,
            size: 9,
            font: regularFont,
            color: rgb(0, 0, 0),
        });

        currentY -= 12;
    }

    currentY -= 8;

    // Draw product lines section if there are any products
    if (productLines && productLines.length > 0) {
        currentY -= 10;
        
        page.drawText('Overige producten/diensten:', {
            x,
            y: currentY,
            size: 11,
            font: boldFont,
            color: rgb(0, 0, 0),
        });
        currentY -= 13;
        
        for (const productLine of productLines) {
            const quantityText = `${productLine.quantity} ${productLine.unit || ''}`;

            page.drawText(productLine.productName || 'Onbekend', {
                x,
                y: currentY,
                size: 9,
                font: regularFont,
                color: rgb(0, 0, 0),
            });

            const quantityWidth = regularFont.widthOfTextAtSize(quantityText, 9);
            page.drawText(quantityText, {
                x: weightValueX - quantityWidth,
                y: currentY,
                size: 9,
                font: regularFont,
                color: rgb(0, 0, 0),
            });

            currentY -= 12;
        }

        currentY -= 8;
    }

    return currentY;
}

/**
 * Main function to generate the weight ticket PDF
 */
export async function generateWeightTicketPdf(data: WeightTicketPdfData): Promise<Uint8Array> {
    const pdfDoc = await PDFDocument.create();
    const page = pdfDoc.addPage([595.28, 841.89]); // A4 size in points
    const { width, height } = page.getSize();

    // Draw header
    await drawHeader(page, pdfDoc);

    // Draw title
    await drawTitle(page, pdfDoc, data.weightTicket.number);

    // Main content area starts here
    let contentY = height - 250;

    await drawField(
        page,
        pdfDoc,
        40,
        contentY,
        'Kenteken:',
        data.weightTicket.truckLicensePlate
    );
    contentY -= 15;
    await drawField(
        page,
        pdfDoc,
        40,
        contentY,
        'Vervoerder:',
        data.carrierParty?.name || ''
    );
    contentY -= 15;

    if (data.weightTicket.weightedAt) {
        await drawField(
            page,
            pdfDoc,
            40,
            contentY,
            'Weegdatum:',
            data.weightTicket.weightedAt
        );
        contentY -= 15;
    }

    await drawField(
        page,
        pdfDoc,
        40,
        contentY,
        'Gemaakt op:',
        data.weightTicket.createdAt
    );
    contentY -= 20;

    // Draw a separator line
    page.drawLine({
        start: { x: 40, y: contentY },
        end: { x: width - 40, y: contentY },
        thickness: 0.5,
        color: rgb(0.8, 0.8, 0.8),
    });
    contentY -= 15;

    // Draw parties section in two columns
    const leftColumnX = 40;
    const rightColumnX = width / 2 + 20;

    // Left column: Consignor and Pickup Location
    await drawPartySection(page, pdfDoc, leftColumnX, contentY, 'Opdrachtgever', data.consignorParty);

    if (data.weightTicket.direction == 'INBOUND' && data.pickupLocation) {
        await drawPartySection(page, pdfDoc, rightColumnX, contentY, 'Laadadres', data.pickupLocation);
    } else if (data.weightTicket.direction == 'OUTBOUND' && data.deliveryLocation) {
        await drawPartySection(page, pdfDoc, rightColumnX, contentY, 'Losadres', data.deliveryLocation);
    }
    contentY -= 70;

    // Draw separator
    page.drawLine({
        start: { x: 40, y: contentY },
        end: { x: width - 40, y: contentY },
        thickness: 0.5,
        color: rgb(0.8, 0.8, 0.8),
    });
    contentY -= 15;

    // Draw weight ticket lines if available
    if (data.lines && data.lines.length > 0) {
        contentY = await drawLinesTable(
            page,
            pdfDoc,
            40,
            contentY,
            data.lines,
            data.productLines || [],
            data.weightTicket.weging1,
            data.weightTicket.weging2,
            data.weightTicket.grossWeight,
            data.weightTicket.tarraWeight,
            data.weightTicket.nettoWeight,
            data.weightTicket.weightUnit
        );
        contentY -= 20;

        // Draw separator after lines
        page.drawLine({
            start: { x: 40, y: contentY },
            end: { x: width - 40, y: contentY },
            thickness: 0.5,
            color: rgb(0.8, 0.8, 0.8),
        });
        contentY -= 20;
    }

    // Draw reclamation section if available
    if (data.weightTicket.reclamation) {
        contentY -= 10;
        const regularFont = await pdfDoc.embedFont(StandardFonts.Helvetica);
        const boldFont = await pdfDoc.embedFont(StandardFonts.HelveticaBold);

        page.drawText('Reclamatie:', {
            x: 40,
            y: contentY,
            size: 11,
            font: boldFont,
            color: rgb(0, 0, 0),
        });
        contentY -= 15;

        const maxWidth = width - 80;
        const reclamationLines = wrapText(data.weightTicket.reclamation, maxWidth, 9);

        for (const line of reclamationLines) {
            page.drawText(line, {
                x: 40,
                y: contentY,
                size: 9,
                font: regularFont,
                color: rgb(0, 0, 0),
            });
            contentY -= 12;
        }
    }

    // Draw footer
    const footerY = 50;
    const regularFont = await pdfDoc.embedFont(StandardFonts.Helvetica);
    const dateString = new Date().toLocaleString('nl-NL', {
        day: '2-digit',
        month: '2-digit',
        year: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        hour12: false
    });
    page.drawText(`Gegenereerd op ${dateString}`, {
        x: 40,
        y: footerY,
        size: 8,
        font: regularFont,
        color: rgb(0.5, 0.5, 0.5),
    });

    return await pdfDoc.save();
}

/**
 * Helper function to wrap text into multiple lines
 */
function wrapText(text: string, maxWidth: number, fontSize: number): string[] {
    // Simple word wrapping - approximate character width
    const avgCharWidth = fontSize * 0.5;
    const maxChars = Math.floor(maxWidth / avgCharWidth);

    const words = text.split(' ');
    const lines: string[] = [];
    let currentLine = '';

    for (const word of words) {
        const testLine = currentLine ? `${currentLine} ${word}` : word;
        if (testLine.length <= maxChars) {
            currentLine = testLine;
        } else {
            if (currentLine) {
                lines.push(currentLine);
            }
            currentLine = word;
        }
    }

    if (currentLine) {
        lines.push(currentLine);
    }

    return lines;
}
