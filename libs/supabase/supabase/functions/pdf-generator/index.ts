import {PDFDocument, PageSizes} from 'npm:pdf-lib';
import {fetchTransportData, TransportData} from './db.ts';
import {drawBackgroundWaybill, drawData} from './pdf.ts';
import {drawSignatures} from './signatures.ts';
import {generateFileName, uploadFile, fetchAllSignatures} from './storage.ts';
import {triggerEmail} from "./email.ts";

// Type definitions
type ApiResponse = {
    success: boolean;
    message: string;
    fileName?: string;
    error?: string;
};

export type SigneeInfo = {
    type: string;
    signature?: string;
    signedAt?: Date | string;
    email?: string;
};

/**
 * Extract signee information based on party type
 * @param transportData - The transport data containing signatures
 * @param partyType - The type of party (consignor, consignee, etc.)
 * @returns Object containing signature, signed timestamp and email
 */
export function getSigneeInfo(partyType: string, transportData?: TransportData, signatures?: Record<string, string | undefined>): SigneeInfo {
    switch (partyType) {
        case 'consignor':
            return {
                type: 'consignor',
                signature: signatures?.consignor_signature || transportData?.signatures.consignor_signature,
                signedAt: transportData?.signatures.consignor_signed_at,
                email: transportData?.signatures.consignor_email
            };
        case 'consignee':
            return {
                type: 'consignee',
                signature: signatures?.consignee_signature || transportData?.signatures.consignee_signature,
                signedAt: transportData?.signatures.consignee_signed_at,
                email: transportData?.signatures.consignee_email
            };
        case 'pickup':
            return {
                type: 'pickup',
                signature: signatures?.pickup_signature || transportData?.signatures.pickup_signature,
                signedAt: transportData?.signatures.pickup_signed_at,
                email: transportData?.signatures.pickup_email
            };
        case 'carrier':
            return {
                type: 'carrier',
                signature: signatures?.carrier_signature || transportData?.signatures.carrier_signature,
                signedAt: transportData?.signatures.carrier_signed_at,
                email: transportData?.signatures.carrier_email
            };
        case 'empty':
            return {
                type: 'empty',
                signature: '',
                signedAt: '',
                email: ''
            };
        default:
            throw new Error('Invalid party type');
    }
}

/**
 * Generate a PDF document from transport data
 * @param transportData - Data to be included in the PDF
 * @returns Promise resolving to PDF binary content
 */
async function generatePdf(transportData: TransportData, signeeInfo: SigneeInfo, signatures?: Record<string, string | undefined>): Promise<Uint8Array> {
    const pdfDoc = await PDFDocument.create();
    const page = pdfDoc.addPage(PageSizes.A4);

    await drawBackgroundWaybill(page, pdfDoc);
    drawData(page, transportData);

    if (signeeInfo.type !== 'empty') {
        await drawSignatures(page, pdfDoc, transportData, signatures);
    }

    return await pdfDoc.save();
}

/**
 * Create a standardized API response
 * @param status - HTTP status code
 * @param body - Response body object
 * @returns Response object
 */
function createApiResponse(status: number, body: ApiResponse): Response {
    return new Response(
        JSON.stringify(body),
        {
            status,
            headers: {"Content-Type": "application/json"}
        }
    );
}

/**
 * Extract transport ID and party type from request body
 * @param req - Request object
 * @returns Object containing transportId and partyType
 */
async function extractRequestData(req: Request): Promise<{ transportId: string, partyType: string }> {
    const body = await req.json();
    return {
        transportId: body.transportId,
        partyType: body.partyType
    };
}

// Main server handler
Deno.serve(async (req) => {
    try {
        const {transportId, partyType} = await extractRequestData(req);
        console.log(`Generating pdf for the party ${partyType} of transport ${transportId}`);
        const {data: transportData, response} = await fetchTransportData(transportId);
        console.log(`[${transportId}] Transport data: ${JSON.stringify(transportData?.transport)}`);
        if (response) return response;

        if (!transportData) {
            return createApiResponse(404, {
                success: false,
                message: 'Resource not found',
                error: 'No transport data available'
            });
        }

        // Fetch signatures from bucket
        console.log(`[${transportId}] Fetching signatures from bucket...`);
        const signatures = await fetchAllSignatures(transportId);
        
        const signeeInfo = getSigneeInfo(partyType, transportData, signatures);

        console.log(`[${transportId}] Transport found, generating pdf...`);
        const pdfBytes = await generatePdf(transportData, signeeInfo, signatures);
        const fileName = generateFileName(transportData, signeeInfo);
        console.log(`[${transportId}] Generated pdf, uploading to storage...`);
        await uploadFile(pdfBytes, fileName);
        console.log(`[${transportId}] Uploaded pdf to storage...`);
        // Call triggerEmail asynchronously without waiting for the result
        if (signeeInfo.email) {
            triggerEmail(signeeInfo, fileName).catch(error => {
                console.error('Email trigger error (non-blocking):', error);
            });
            console.log(`[${transportId}] Email triggered...`);
        }

        console.log(`[${transportId}] PDF successfully generated and stored`);
        // Return success response with signee info
        return createApiResponse(201, {
            success: true,
            message: 'PDF successfully generated and stored',
            fileName
        });
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
