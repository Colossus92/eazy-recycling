import { supabase } from '@/api/supabaseClient';
import { format } from 'date-fns';

export interface InvoicePdfInfo {
  fileName: string;
  filePath: string;
  downloadUrl: string;
  timestamp: string;
  fileSizeKb: number;
}

/**
 * Fetches the invoice PDF information for a given storage path from Supabase storage
 * @param storagePath - The storage path of the invoice PDF (e.g., "companyCode/invoice-123.pdf")
 * @returns InvoicePdfInfo object with file details or null if no PDF found
 * @throws Error if there's an issue fetching the invoice PDF
 */
export const fetchInvoicePdfInfo = async (
  storagePath: string
): Promise<InvoicePdfInfo | null> => {
  try {
    // Extract directory and filename from the storage path
    const pathParts = storagePath.split('/');
    const fileName = pathParts.pop() || '';
    const directory = pathParts.join('/');

    const { data: files, error: listError } = await supabase.storage
      .from('invoices')
      .list(directory, {
        search: fileName,
      });

    if (listError) {
      console.error('Error listing invoice PDF files:', listError);
      throw new Error('Fout bij het ophalen van factuur PDF bestanden');
    }

    const file = files?.find((f) => f.name === fileName);
    if (!file) {
      return null;
    }

    const { data: signedUrlData, error: urlError } = await supabase.storage
      .from('invoices')
      .createSignedUrl(storagePath, 3600); // 1 hour expiry

    if (urlError) {
      console.error('Error creating signed URL:', urlError);
      throw new Error('Fout bij het genereren van download link');
    }

    return {
      timestamp: format(new Date(file.updated_at), 'dd-MM-yyyy HH:mm'),
      fileSizeKb: file.metadata?.size
        ? Math.round(file.metadata.size / 1024)
        : 0,
      fileName: file.name,
      filePath: storagePath,
      downloadUrl: signedUrlData.signedUrl,
    };
  } catch (error) {
    console.error('Unexpected error fetching invoice PDF info:', error);
    throw error;
  }
};

/**
 * Triggers a download of the invoice PDF file
 * @param invoicePdfInfo - The invoice PDF information containing the download URL
 * @param invoiceNumber - The invoice number (used in filename)
 */
export const downloadInvoicePdf = (
  invoicePdfInfo: InvoicePdfInfo,
  invoiceNumber: string
): void => {
  const link = document.createElement('a');
  link.href = invoicePdfInfo.downloadUrl;
  link.download = `factuur-${invoiceNumber}.pdf`;
  link.target = '_blank';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
};

/**
 * Downloads an invoice PDF directly from Supabase storage using the storage path
 * @param pdfUrl - The storage path of the invoice PDF (e.g., "companyCode/invoice-123.pdf")
 * @param fileName - The filename to use for the downloaded file
 * @throws Error if there's an issue downloading the invoice PDF
 */
export const downloadInvoicePdfDirect = async (
  pdfUrl: string,
  fileName: string
): Promise<void> => {
  const { data: pdfData, error } = await supabase.storage
    .from('invoices')
    .download(pdfUrl);

  if (error) {
    console.error('Error downloading invoice PDF:', error);
    throw new Error('Fout bij het downloaden van factuur PDF');
  }

  const url = URL.createObjectURL(pdfData);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
};

/**
 * Opens an invoice PDF in a new browser tab using the storage path
 * @param pdfUrl - The storage path of the invoice PDF (e.g., "companyCode/invoice-123.pdf")
 * @throws Error if there's an issue opening the invoice PDF
 */
export const openInvoicePdfInNewTab = async (
  pdfUrl: string
): Promise<void> => {
  const { data: signedUrlData, error } = await supabase.storage
    .from('invoices')
    .createSignedUrl(pdfUrl, 3600); // 1 hour expiry

  if (error) {
    console.error('Error creating signed URL:', error);
    throw new Error('Fout bij het ophalen van factuur PDF');
  }

  // Open in new tab
  window.open(signedUrlData.signedUrl, '_blank');
};
