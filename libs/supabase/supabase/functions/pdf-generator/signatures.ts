import { PDFDocument, rgb } from 'npm:pdf-lib'
import type { PDFPage } from 'npm:pdf-lib'
import { TransportData } from './db.ts'

/**
 * Extracts base64 data from a data URL string
 * @param dataUrl Data URL in format data:image/png;base64,{base64data}
 * @returns Base64 data without the prefix
 */
function extractBase64FromDataUrl(dataUrl?: string): string | null {
    if (!dataUrl) return null;

    const matches = dataUrl.match(/^data:image\/(png|jpeg|jpg|gif);base64,(.+)$/);
    return matches && matches.length >= 3 ? matches[2] : null;
}

/**
 * Draw a signature on the PDF page
 * @param page PDF page to draw on
 * @param pdfDoc PDF document
 * @param x X coordinate
 * @param y Y coordinate
 * @param signature Signature data URL
 */
async function drawSignature(page: PDFPage, pdfDoc: PDFDocument, x: number, y: number, signature?: string) {
    if (signature) {
        const base64Data = extractBase64FromDataUrl(signature);
        if (base64Data) {
            const signatureBytes = Uint8Array.from(atob(base64Data), c => c.charCodeAt(0));
            const signatureImage = await pdfDoc.embedPng(signatureBytes);
            page.drawImage(signatureImage, {
                x,
                y,
                width: 100,
                height: 80
            });
        }
    }
}

/**
 * Draw all signatures on the PDF page
 * @param page PDF page to draw on
 * @param pdfDoc PDF document
 * @param transportData Transport data containing signatures
 * @param bucketSignatures Optional signatures fetched from bucket
 */
export async function drawSignatures(
    page: PDFPage, 
    pdfDoc: PDFDocument, 
    transportData: TransportData,
    bucketSignatures?: Record<string, string | undefined>
) {
    // Use bucket signatures if available, otherwise fall back to database signatures
    const consignorSignature = bucketSignatures?.consignor_signature || transportData.signatures.consignor_signature;
    const pickupSignature = bucketSignatures?.pickup_signature || transportData.signatures.pickup_signature;
    const carrierSignature = bucketSignatures?.carrier_signature || transportData.signatures.carrier_signature;
    const consigneeSignature = bucketSignatures?.consignee_signature || transportData.signatures.consignee_signature;

    await drawSignature(page, pdfDoc, 50, 0, consignorSignature);
    await drawSignature(page, pdfDoc, 180, 0, pickupSignature);
    await drawSignature(page, pdfDoc, 310, 0, carrierSignature);
    await drawSignature(page, pdfDoc, 440, 0, consigneeSignature);
}
