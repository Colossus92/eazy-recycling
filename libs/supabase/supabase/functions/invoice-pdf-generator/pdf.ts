import type { PDFFont, PDFPage } from 'npm:pdf-lib';
import { PDFDocument, rgb, StandardFonts } from 'npm:pdf-lib';
import { formatCurrency as baseCurrency, formatDate, InvoiceData } from './types.ts';

/**
 * Format currency with optional negation for credit notes
 */
function formatCurrency(amount: number, isCreditNote = false): string {
  // For credit notes, negate the value (except when 0)
  const displayValue = isCreditNote && amount !== 0 ? -amount : amount;
  return baseCurrency(displayValue);
}

// Constants for layout
const PAGE_WIDTH = 595.28; // A4 width in points
const PAGE_HEIGHT = 841.89; // A4 height in points
const MARGIN_LEFT = 50;
const MARGIN_RIGHT = 50;
const MARGIN_TOP = 50;
const MARGIN_BOTTOM = 80;
const CONTENT_WIDTH = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT;

// Minimum Y position before requiring a new page (above footer area)
const MIN_Y_BEFORE_NEW_PAGE = MARGIN_BOTTOM + 100;

// Context for multi-page rendering
interface PageContext {
  pdfDoc: PDFDocument;
  pages: PDFPage[];
  currentPage: PDFPage;
  currentPageIndex: number;
  fonts: { regular: PDFFont; bold: PDFFont };
  data: InvoiceData;
}

// Colors - all black/grayscale for professional B2B look
const COLOR_BLACK = rgb(0, 0, 0);
const COLOR_DARK_GRAY = rgb(0.3, 0.3, 0.3);
const COLOR_LIGHT_GRAY = rgb(0.6, 0.6, 0.6);
const COLOR_LINE = rgb(0.85, 0.85, 0.85);

/**
 * Truncate text with ellipsis if it exceeds maxLength
 */
function truncateText(text: string, maxLength: number): string {
  if (text.length <= maxLength) return text;
  return text.substring(0, maxLength - 3) + '...';
}

/**
 * Draw the header section with logo and tenant info
 */
async function drawHeader(
  page: PDFPage,
  pdfDoc: PDFDocument,
  data: InvoiceData,
  fonts: { regular: PDFFont; bold: PDFFont }
): Promise<number> {
  const { height } = page.getSize();
  const currentY = height - MARGIN_TOP;

  // Logo on the left
  const logoX = MARGIN_LEFT;
  const logoY = currentY - 50;
  const logoMaxWidth = 120;
  const logoMaxHeight = 50;

  // Load and embed the logo
  try {
    const logoPath = new URL('./logo.png', import.meta.url);
    const logoBytes = await Deno.readFile(logoPath);
    const logoImage = await pdfDoc.embedPng(logoBytes);

    // Scale logo to fit within max dimensions while maintaining aspect ratio
    const logoScale = Math.min(
      logoMaxWidth / logoImage.width,
      logoMaxHeight / logoImage.height
    );
    const scaledWidth = logoImage.width * logoScale;
    const scaledHeight = logoImage.height * logoScale;

    page.drawImage(logoImage, {
      x: logoX,
      y: logoY + (logoMaxHeight - scaledHeight), // Align to top
      width: scaledWidth,
      height: scaledHeight,
    });
  } catch (error) {
    console.error('Failed to load logo:', error);
    // Fallback: draw placeholder
    page.drawRectangle({
      x: logoX,
      y: logoY,
      width: logoMaxWidth,
      height: logoMaxHeight,
      borderColor: COLOR_LINE,
      borderWidth: 1,
    });
    page.drawText('LOGO', {
      x: logoX + 45,
      y: logoY + 20,
      size: 12,
      font: fonts.bold,
      color: COLOR_LIGHT_GRAY,
    });
  }

  // Tenant address on the right
  const rightX = PAGE_WIDTH - MARGIN_RIGHT;
  let addressY = currentY;

  // Company name
  page.drawText(data.tenant.name, {
    x: rightX - 200,
    y: addressY,
    size: 10,
    font: fonts.bold,
    color: COLOR_BLACK,
  });

  addressY -= 14;
  page.drawText(`${data.tenant.address.street} ${data.tenant.address.buildingNumber}`, {
    x: rightX - 200,
    y: addressY,
    size: 9,
    font: fonts.regular,
    color: COLOR_BLACK,
  });

  addressY -= 12;
  page.drawText(`${data.tenant.address.postalCode} ${data.tenant.address.city}`, {
    x: rightX - 200,
    y: addressY,
    size: 9,
    font: fonts.regular,
    color: COLOR_BLACK,
  });

  addressY -= 12;
  page.drawText(data.tenant.phone, {
    x: rightX - 200,
    y: addressY,
    size: 9,
    font: fonts.regular,
    color: COLOR_BLACK,
  });

  addressY -= 12;
  page.drawText(data.tenant.email, {
    x: rightX - 200,
    y: addressY,
    size: 9,
    font: fonts.regular,
    color: COLOR_BLACK,
  });

  return currentY - 80;
}

/**
 * Draw the invoice title and type
 */
function drawInvoiceTitle(
  page: PDFPage,
  data: InvoiceData,
  startY: number,
  fonts: { regular: PDFFont; bold: PDFFont }
): number {
  let currentY = startY - 30;

  // Invoice type as main title
  let titleText: string;
  if (data.invoiceType === 'CREDITFACTUUR') {
    titleText = 'Creditfactuur';
  } else if (data.invoiceType === 'INKOOPFACTUUR') {
    titleText = 'Inkoopfactuur';
  } else {
    titleText = 'Verkoopfactuur';
  }

  page.drawText(titleText, {
    x: MARGIN_LEFT,
    y: currentY,
    size: 22,
    font: fonts.bold,
    color: COLOR_BLACK,
  });

  // If inkoopfactuur, add the required line
  if (data.invoiceType === 'INKOOPFACTUUR') {
    currentY -= 18;
    page.drawText('Inkoopfactuur uitgereikt door afnemer', {
      x: MARGIN_LEFT,
      y: currentY,
      size: 9,
      font: fonts.regular,
      color: COLOR_DARK_GRAY,
    });
  }

  // If creditfactuur, add reference to original invoice
  if (data.invoiceType === 'CREDITFACTUUR' && data.creditedInvoiceNumber) {
    currentY -= 18;
    page.drawText(`Creditfactuur voor factuur ${data.creditedInvoiceNumber}`, {
      x: MARGIN_LEFT,
      y: currentY,
      size: 9,
      font: fonts.regular,
      color: COLOR_DARK_GRAY,
    });
  }

  return currentY - 25;
}

/**
 * Draw the customer and invoice details section
 */
function drawDetailsSection(
  page: PDFPage,
  data: InvoiceData,
  startY: number,
  fonts: { regular: PDFFont; bold: PDFFont }
): number {
  let currentY = startY;
  const leftColumnX = MARGIN_LEFT;
  const rightColumnX = PAGE_WIDTH / 2 + 20;

  // Left column: Customer details
  page.drawText('Factuuradres', {
    x: leftColumnX,
    y: currentY,
    size: 9,
    font: fonts.bold,
    color: COLOR_DARK_GRAY,
  });

  currentY -= 16;
  page.drawText(data.customer.name, {
    x: leftColumnX,
    y: currentY,
    size: 10,
    font: fonts.bold,
    color: COLOR_BLACK,
  });

  currentY -= 14;
  page.drawText(`${data.customer.address.street} ${data.customer.address.buildingNumber}`, {
    x: leftColumnX,
    y: currentY,
    size: 9,
    font: fonts.regular,
    color: COLOR_BLACK,
  });

  currentY -= 12;
  page.drawText(`${data.customer.address.postalCode} ${data.customer.address.city}`, {
    x: leftColumnX,
    y: currentY,
    size: 9,
    font: fonts.regular,
    color: COLOR_BLACK,
  });

  if (data.customer.address.country) {
    currentY -= 12;
    page.drawText(data.customer.address.country, {
      x: leftColumnX,
      y: currentY,
      size: 9,
      font: fonts.regular,
      color: COLOR_BLACK,
    });
  }

  // Right column: Invoice metadata
  let metaY = startY;
  const labelX = rightColumnX;
  const valueX = rightColumnX + 100;

  // Factuurnummer
  page.drawText('Factuurnummer', {
    x: labelX,
    y: metaY,
    size: 9,
    font: fonts.regular,
    color: COLOR_DARK_GRAY,
  });
  page.drawText(data.invoiceNumber, {
    x: valueX,
    y: metaY,
    size: 9,
    font: fonts.bold,
    color: COLOR_BLACK,
  });

  metaY -= 14;
  // Factuurdatum
  page.drawText('Factuurdatum', {
    x: labelX,
    y: metaY,
    size: 9,
    font: fonts.regular,
    color: COLOR_DARK_GRAY,
  });
  page.drawText(formatDate(data.invoiceDate), {
    x: valueX,
    y: metaY,
    size: 9,
    font: fonts.regular,
    color: COLOR_BLACK,
  });

  metaY -= 14;
  // Crediteurnummer
  page.drawText('Crediteurnummer', {
    x: labelX,
    y: metaY,
    size: 9,
    font: fonts.regular,
    color: COLOR_DARK_GRAY,
  });
  page.drawText(data.customer.creditorNumber, {
    x: valueX,
    y: metaY,
    size: 9,
    font: fonts.regular,
    color: COLOR_BLACK,
  });

  metaY -= 14;
  // BTW nummer (customer)
  if (data.customer.vatNumber) {
    page.drawText('BTW-nummer', {
      x: labelX,
      y: metaY,
      size: 9,
      font: fonts.regular,
      color: COLOR_DARK_GRAY,
    });
    page.drawText(data.customer.vatNumber, {
      x: valueX,
      y: metaY,
      size: 9,
      font: fonts.regular,
      color: COLOR_BLACK,
    });
    metaY -= 14;
  }

  // Betaaltermijn
  page.drawText('Betaaltermijn', {
    x: labelX,
    y: metaY,
    size: 9,
    font: fonts.regular,
    color: COLOR_DARK_GRAY,
  });
  page.drawText(`${data.paymentTermDays} dagen`, {
    x: valueX,
    y: metaY,
    size: 9,
    font: fonts.regular,
    color: COLOR_BLACK,
  });

  return Math.min(currentY, metaY) - 30;
}

/**
 * Calculate the height needed for the totals section
 */
function calculateTotalsHeight(data: InvoiceData): number {
  // Invoice totals: header + excl VAT + BTW + separator + incl VAT = ~60pt
  const invoiceTotalsHeight = 60;
  // Material totals: header + rows (12pt each) + some padding
  const materialTotalsHeight = 14 + (data.materialTotals.length * 12) + 20;
  return Math.max(invoiceTotalsHeight, materialTotalsHeight);
}

/**
 * Draw the totals section with material totals table on the left
 * Now accepts PageContext to support starting a new page if needed
 */
function drawTotalsMultiPage(
  ctx: PageContext,
  startY: number
): { currentY: number; currentPage: PDFPage } {
  const { fonts, data } = ctx;
  let currentPage = ctx.pages[ctx.pages.length - 1];

  // Calculate if totals section fits on current page
  const totalsHeight = calculateTotalsHeight(data);
  let currentY = startY;

  // Check if we need a new page for totals
  if (currentY - totalsHeight < MARGIN_BOTTOM + 60) {
    // Create new page for totals
    currentPage = ctx.pdfDoc.addPage([PAGE_WIDTH, PAGE_HEIGHT]);
    ctx.pages.push(currentPage);

    // Draw continuation header
    currentY = drawContinuationHeader(currentPage, data, fonts);
  }

  // === Left side: Material totals table ===
  // Use similar spacing as invoice lines table (55pt between Datum and Omschrijving)
  const matColMaterial = MARGIN_LEFT;
  const matColWeightEnd = MARGIN_LEFT + 165;
  const matColAmountEnd = MARGIN_LEFT + 240;

  const totalsStartY = currentY;

  // Material totals header
  currentPage.drawText('Totalen per materiaal', { x: matColMaterial, y: currentY, size: 8, font: fonts.bold, color: COLOR_DARK_GRAY });
  const weightHeader = 'Totaal gewicht';
  const weightHeaderWidth = fonts.bold.widthOfTextAtSize(weightHeader, 8);
  currentPage.drawText(weightHeader, { x: matColWeightEnd - weightHeaderWidth, y: currentY, size: 8, font: fonts.bold, color: COLOR_DARK_GRAY });
  const amountHeader = 'Totaalbedrag';
  const amountHeaderWidth = fonts.bold.widthOfTextAtSize(amountHeader, 8);
  currentPage.drawText(amountHeader, { x: matColAmountEnd - amountHeaderWidth, y: currentY, size: 8, font: fonts.bold, color: COLOR_DARK_GRAY });

  // === Right side: Invoice totals ===
  const labelX = PAGE_WIDTH - MARGIN_RIGHT - 180;
  const rightAlignX = PAGE_WIDTH - MARGIN_RIGHT;

  const isCreditNote = data.invoiceType === 'CREDITFACTUUR';

  currentPage.drawText('Totaal excl. BTW', { x: labelX, y: currentY, size: 9, font: fonts.regular, color: COLOR_DARK_GRAY });
  const exclVatText = formatCurrency(data.totals.totalExclVat, isCreditNote);
  const exclVatWidth = fonts.regular.widthOfTextAtSize(exclVatText, 9);
  currentPage.drawText(exclVatText, { x: rightAlignX - exclVatWidth, y: currentY, size: 9, font: fonts.regular, color: COLOR_BLACK });

  currentY -= 14;

  currentPage.drawText('BTW', { x: labelX, y: currentY, size: 9, font: fonts.regular, color: COLOR_DARK_GRAY });
  const vatText = formatCurrency(data.totals.vatAmount, isCreditNote);
  const vatWidth = fonts.regular.widthOfTextAtSize(vatText, 9);
  currentPage.drawText(vatText, { x: rightAlignX - vatWidth, y: currentY, size: 9, font: fonts.regular, color: COLOR_BLACK });

  // Material totals rows (left side)
  let matY = totalsStartY - 14;
  for (const mat of data.materialTotals) {
    const truncatedMaterial = truncateText(mat.material, 28);
    currentPage.drawText(truncatedMaterial, { x: matColMaterial, y: matY, size: 8, font: fonts.regular, color: COLOR_BLACK });
    const weightText = `${mat.totalWeight} ${mat.unit}`;
    const weightWidth = fonts.regular.widthOfTextAtSize(weightText, 8);
    currentPage.drawText(weightText, { x: matColWeightEnd - weightWidth, y: matY, size: 8, font: fonts.regular, color: COLOR_BLACK });
    const amountText = formatCurrency(mat.totalAmount, isCreditNote);
    const amountWidth = fonts.regular.widthOfTextAtSize(amountText, 8);
    currentPage.drawText(amountText, { x: matColAmountEnd - amountWidth, y: matY, size: 8, font: fonts.regular, color: COLOR_BLACK });
    matY -= 12;
  }

  currentY -= 14;

  // Separator line before total
  currentPage.drawLine({
    start: { x: labelX, y: currentY + 4 },
    end: { x: PAGE_WIDTH - MARGIN_RIGHT, y: currentY + 4 },
    thickness: 1,
    color: COLOR_BLACK,
  });

  currentY -= 12;

  // Totaal incl. BTW
  currentPage.drawText('Totaal incl. BTW', { x: labelX, y: currentY, size: 11, font: fonts.bold, color: COLOR_BLACK });
  const totalText = formatCurrency(data.totals.totalInclVat, isCreditNote);
  const totalWidth = fonts.bold.widthOfTextAtSize(totalText, 11);
  currentPage.drawText(totalText, { x: rightAlignX - totalWidth, y: currentY, size: 11, font: fonts.bold, color: COLOR_BLACK });

  // Return the lowest Y position
  const matTableBottom = totalsStartY - 14 - (data.materialTotals.length * 12);
  return { currentY: Math.min(currentY - 30, matTableBottom - 20), currentPage };
}

/**
 * Draw the conditions text
 */
function drawConditions(
  page: PDFPage,
  startY: number,
  fonts: { regular: PDFFont; bold: PDFFont }
): number {
  let currentY = startY;

  const conditionsText = 'Op al onze offertes, op alle opdrachten aan ons en op alle met ons gesloten overeenkomsten zijn onze Algemene Voorwaarden van toepassing. Deze Algemene Voorwaarden staan op de achterzijde van ons briefpapier vermeld, op onze website gepubliceerd en worden u op verzoek toegezonden. Uitdrukkelijk worden andersluidende voorwaarden afgewezen.';

  // Wrap text manually
  const maxWidth = CONTENT_WIDTH;
  const fontSize = 7;
  const avgCharWidth = fontSize * 0.45;
  const maxChars = Math.floor(maxWidth / avgCharWidth);

  const words = conditionsText.split(' ');
  const lines: string[] = [];
  let currentLine = '';

  for (const word of words) {
    const testLine = currentLine ? `${currentLine} ${word}` : word;
    if (testLine.length <= maxChars) {
      currentLine = testLine;
    } else {
      if (currentLine) lines.push(currentLine);
      currentLine = word;
    }
  }
  if (currentLine) lines.push(currentLine);

  for (const line of lines) {
    page.drawText(line, {
      x: MARGIN_LEFT,
      y: currentY,
      size: fontSize,
      font: fonts.regular,
      color: COLOR_LIGHT_GRAY,
    });
    currentY -= 10;
  }

  return currentY - 10;
}

/**
 * Draw the footer with tenant details
 */
function drawFooter(
  page: PDFPage,
  data: InvoiceData,
  fonts: { regular: PDFFont; bold: PDFFont }
): void {
  const footerY = MARGIN_BOTTOM - 30;

  // Separator line
  page.drawLine({
    start: { x: MARGIN_LEFT, y: footerY + 20 },
    end: { x: PAGE_WIDTH - MARGIN_RIGHT, y: footerY + 20 },
    thickness: 0.5,
    color: COLOR_LINE,
  });

  // Footer content - single line with all details
  const footerParts = [
    data.tenant.name,
    `${data.tenant.address.street} ${data.tenant.address.buildingNumber}`,
    `${data.tenant.address.postalCode} ${data.tenant.address.city}`,
    `T: ${data.tenant.phone}`,
    data.tenant.email,
    data.tenant.website,
  ];

  const footerLine1 = footerParts.join('  •  ');

  page.drawText(footerLine1, {
    x: MARGIN_LEFT,
    y: footerY,
    size: 7,
    font: fonts.regular,
    color: COLOR_DARK_GRAY,
  });

  // Second line with business details
  const footerLine2 = `KvK: ${data.tenant.kvkNumber}  •  IBAN: ${data.tenant.ibanNumber}  •  BTW: ${data.tenant.vatNumber}`;

  page.drawText(footerLine2, {
    x: MARGIN_LEFT,
    y: footerY - 12,
    size: 7,
    font: fonts.regular,
    color: COLOR_DARK_GRAY,
  });
}

/**
 * Draw continuation header for pages after the first
 */
function drawContinuationHeader(
  page: PDFPage,
  data: InvoiceData,
  fonts: { regular: PDFFont; bold: PDFFont }
): number {
  const { height } = page.getSize();
  const currentY = height - MARGIN_TOP;

  // Invoice number and continuation text
  page.drawText(`${data.invoiceType} ${data.invoiceNumber}`, {
    x: MARGIN_LEFT,
    y: currentY,
    size: 12,
    font: fonts.bold,
    color: COLOR_BLACK,
  });

  page.drawText('(vervolg)', {
    x: MARGIN_LEFT + fonts.bold.widthOfTextAtSize(`${data.invoiceType} ${data.invoiceNumber}`, 12) + 10,
    y: currentY,
    size: 10,
    font: fonts.regular,
    color: COLOR_DARK_GRAY,
  });

  return currentY - 30;
}

/**
 * Draw page number in footer area
 */
function drawPageNumber(
  page: PDFPage,
  pageNum: number,
  totalPages: number,
  fonts: { regular: PDFFont; bold: PDFFont }
): void {
  const pageText = `Pagina ${pageNum} van ${totalPages}`;
  const textWidth = fonts.regular.widthOfTextAtSize(pageText, 8);

  page.drawText(pageText, {
    x: PAGE_WIDTH - MARGIN_RIGHT - textWidth,
    y: MARGIN_BOTTOM - 45,
    size: 8,
    font: fonts.regular,
    color: COLOR_DARK_GRAY,
  });
}

/**
 * Draw the invoice lines table with multi-page support
 * Returns { currentY, currentPage, pageIndex }
 */
function drawLinesTableMultiPage(
  ctx: PageContext,
  startY: number
): { currentY: number; currentPage: PDFPage; pageIndex: number } {
  let currentY = startY;
  let { currentPage, currentPageIndex } = ctx;
  const { fonts, data } = ctx;

  // Column positions - expanded Omschrijving, compact other columns
  const colDate = MARGIN_LEFT;           // 50
  const colDesc = MARGIN_LEFT + 55;      // 105
  const colOrder = MARGIN_LEFT + 230;    // 280 - moved right for more desc space
  const colQty = MARGIN_LEFT + 295;      // 345
  const colVat = MARGIN_LEFT + 340;      // 390
  const colPriceEnd = PAGE_WIDTH - MARGIN_RIGHT - 70; // 485 - right edge for price
  const colTotalEnd = PAGE_WIDTH - MARGIN_RIGHT;      // 555 - right edge for total
  const colQtyEnd = colQty + 50;
  const colVatEnd = colVat + 35;

  // Helper function to draw table headers
  const drawTableHeaders = (page: PDFPage, y: number): number => {
    page.drawText('Datum', { x: colDate, y, size: 8, font: fonts.bold, color: COLOR_DARK_GRAY });
    page.drawText('Omschrijving', { x: colDesc, y, size: 8, font: fonts.bold, color: COLOR_DARK_GRAY });
    page.drawText('Ordernr.', { x: colOrder, y, size: 8, font: fonts.bold, color: COLOR_DARK_GRAY });

    const aantalHeader = 'Aantal';
    const aantalHeaderWidth = fonts.bold.widthOfTextAtSize(aantalHeader, 8);
    page.drawText(aantalHeader, { x: colQtyEnd - aantalHeaderWidth, y, size: 8, font: fonts.bold, color: COLOR_DARK_GRAY });

    const btwHeader = 'BTW';
    const btwHeaderWidth = fonts.bold.widthOfTextAtSize(btwHeader, 8);
    page.drawText(btwHeader, { x: colVatEnd - btwHeaderWidth, y, size: 8, font: fonts.bold, color: COLOR_DARK_GRAY });

    const priceHeader = 'PPE';
    const priceHeaderWidth = fonts.bold.widthOfTextAtSize(priceHeader, 8);
    page.drawText(priceHeader, { x: colPriceEnd - priceHeaderWidth, y, size: 8, font: fonts.bold, color: COLOR_DARK_GRAY });

    const totalHeader = 'Totaal';
    const totalHeaderWidth = fonts.bold.widthOfTextAtSize(totalHeader, 8);
    page.drawText(totalHeader, { x: colTotalEnd - totalHeaderWidth, y, size: 8, font: fonts.bold, color: COLOR_DARK_GRAY });

    // Header line
    const lineY = y - 8;
    page.drawLine({
      start: { x: MARGIN_LEFT, y: lineY },
      end: { x: PAGE_WIDTH - MARGIN_RIGHT, y: lineY },
      thickness: 1,
      color: COLOR_BLACK,
    });

    return lineY - 14;
  };

  // Draw initial headers
  currentY = drawTableHeaders(currentPage, currentY);

  // Table rows
  const lineHeight = 10;
  const rowPadding = 4;

  for (const line of data.lines) {
    const descLines = line.description;
    const rowHeight = Math.max(lineHeight, descLines.length * lineHeight) + rowPadding;

    // Check if we need a new page
    if (currentY - rowHeight < MIN_Y_BEFORE_NEW_PAGE) {
      // Draw bottom line on current page
      currentPage.drawLine({
        start: { x: MARGIN_LEFT, y: currentY - 4 },
        end: { x: PAGE_WIDTH - MARGIN_RIGHT, y: currentY - 4 },
        thickness: 0.5,
        color: COLOR_LINE,
      });

      // Add "Vervolg op volgende pagina" text
      currentPage.drawText('Vervolg op volgende pagina...', {
        x: MARGIN_LEFT,
        y: currentY - 20,
        size: 8,
        font: fonts.regular,
        color: COLOR_DARK_GRAY,
      });

      // Create new page
      currentPage = ctx.pdfDoc.addPage([PAGE_WIDTH, PAGE_HEIGHT]);
      ctx.pages.push(currentPage);
      currentPageIndex++;

      // Draw continuation header
      currentY = drawContinuationHeader(currentPage, data, fonts);

      // Draw table headers on new page
      currentY = drawTableHeaders(currentPage, currentY);
    }

    // Draw the row
    currentPage.drawText(formatDate(line.date), { x: colDate, y: currentY, size: 8, font: fonts.regular, color: COLOR_BLACK });

    let descY = currentY;
    for (const descLine of descLines) {
      const truncatedDesc = truncateText(descLine, 42);
      currentPage.drawText(truncatedDesc, { x: colDesc, y: descY, size: 8, font: fonts.regular, color: COLOR_BLACK });
      descY -= lineHeight;
    }

    currentPage.drawText(line.orderNumber || '-', { x: colOrder, y: currentY, size: 8, font: fonts.regular, color: COLOR_BLACK });

    const qtyText = `${line.quantity} ${line.unit}`;
    const qtyWidth = fonts.regular.widthOfTextAtSize(qtyText, 8);
    currentPage.drawText(qtyText, { x: colQtyEnd - qtyWidth, y: currentY, size: 8, font: fonts.regular, color: COLOR_BLACK });

    const vatPctText = line.vatPercentage === 'G' ? 'G' : `${line.vatPercentage}%`;
    const vatPctWidth = fonts.regular.widthOfTextAtSize(vatPctText, 8);
    currentPage.drawText(vatPctText, { x: colVatEnd - vatPctWidth, y: currentY, size: 8, font: fonts.regular, color: COLOR_BLACK });

    const priceText = formatCurrency(line.pricePerUnit); // Keep piece price positive
    const priceWidth = fonts.regular.widthOfTextAtSize(priceText, 8);
    currentPage.drawText(priceText, { x: colPriceEnd - priceWidth, y: currentY, size: 8, font: fonts.regular, color: COLOR_BLACK });

    const isCreditNote = ctx.data.invoiceType === 'CREDITFACTUUR';
    const totalText = formatCurrency(line.totalAmount, isCreditNote);
    const totalWidth = fonts.regular.widthOfTextAtSize(totalText, 8);
    currentPage.drawText(totalText, { x: colTotalEnd - totalWidth, y: currentY, size: 8, font: fonts.regular, color: COLOR_BLACK });

    currentY -= rowHeight;
  }

  // Bottom line
  currentY -= 4;
  currentPage.drawLine({
    start: { x: MARGIN_LEFT, y: currentY },
    end: { x: PAGE_WIDTH - MARGIN_RIGHT, y: currentY },
    thickness: 0.5,
    color: COLOR_LINE,
  });

  return { currentY: currentY - 20, currentPage, pageIndex: currentPageIndex };
}

/**
 * Main function to generate the invoice PDF
 */
export async function generateInvoicePdf(data: InvoiceData): Promise<Uint8Array> {
  const pdfDoc = await PDFDocument.create();
  const firstPage = pdfDoc.addPage([PAGE_WIDTH, PAGE_HEIGHT]);

  // Embed fonts
  const regularFont = await pdfDoc.embedFont(StandardFonts.Helvetica);
  const boldFont = await pdfDoc.embedFont(StandardFonts.HelveticaBold);
  const fonts = { regular: regularFont, bold: boldFont };

  // Create page context for multi-page support
  const ctx: PageContext = {
    pdfDoc,
    pages: [firstPage],
    currentPage: firstPage,
    currentPageIndex: 0,
    fonts,
    data,
  };

  // Draw first page header and details
  let currentY = await drawHeader(firstPage, pdfDoc, data, fonts);
  currentY = drawInvoiceTitle(firstPage, data, currentY, fonts);
  currentY = drawDetailsSection(firstPage, data, currentY, fonts);

  // Draw lines table with multi-page support
  const tableResult = drawLinesTableMultiPage(ctx, currentY);
  currentY = tableResult.currentY;

  // Draw totals with multi-page support (may create new page if needed)
  const totalsResult = drawTotalsMultiPage(ctx, currentY);
  currentY = totalsResult.currentY;
  const currentPage = totalsResult.currentPage;

  drawConditions(currentPage, currentY, fonts);

  // Draw footer and page numbers on all pages
  const totalPages = ctx.pages.length;
  for (let i = 0; i < ctx.pages.length; i++) {
    drawFooter(ctx.pages[i], data, fonts);
    drawPageNumber(ctx.pages[i], i + 1, totalPages, fonts);
  }

  return await pdfDoc.save();
}
