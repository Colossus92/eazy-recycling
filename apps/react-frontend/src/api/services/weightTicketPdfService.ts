import { supabase } from '@/api/supabaseClient';
import { format } from 'date-fns';

export interface WeightTicketPdfInfo {
  fileName: string;
  filePath: string;
  downloadUrl: string;
  timestamp: string;
  fileSizeKb: number;
}

/**
 * Fetches the latest weight ticket PDF information for a given weight ticket ID from Supabase storage
 * @param weightTicketId - The ID of the weight ticket
 * @returns WeightTicketPdfInfo object with file details or null if no PDF found
 * @throws Error if there's an issue fetching the weight ticket PDF
 */
export const fetchWeightTicketPdfInfo = async (
  weightTicketId: string
): Promise<WeightTicketPdfInfo | null> => {
  try {
    const { data: files, error: listError } = await supabase.storage
      .from('weight-tickets')
      .list(`${weightTicketId}`, {
        sortBy: { column: 'created_at', order: 'desc' },
      });

    if (listError) {
      console.error('Error listing weight ticket PDF files:', listError);
      throw new Error('Fout bij het ophalen van weegbon PDF bestanden');
    }

    if (!files || files.length === 0) {
      return null;
    }

    const latestFile = files[0];
    const filePath = `${weightTicketId}/${latestFile.name}`;

    const { data: signedUrlData, error: urlError } = await supabase.storage
      .from('weight-tickets')
      .createSignedUrl(filePath, 3600); // 1 hour expiry

    if (urlError) {
      console.error('Error creating signed URL:', urlError);
      throw new Error('Fout bij het genereren van download link');
    }

    return {
      timestamp: format(new Date(latestFile.updated_at), 'dd-MM-yyyy hh:mm'),
      fileSizeKb: latestFile.metadata?.size
        ? Math.round(latestFile.metadata.size / 1024)
        : 0,
      fileName: latestFile.name,
      filePath,
      downloadUrl: signedUrlData.signedUrl,
    };
  } catch (error) {
    console.error('Unexpected error fetching weight ticket PDF info:', error);
    throw error;
  }
};

/**
 * Triggers a download of the weight ticket PDF file
 * @param weightTicketPdfInfo - The weight ticket PDF information containing the download URL
 * @param weightTicketId - The ID of the weight ticket (used in filename)
 */
export const downloadWeightTicketPdf = (
  weightTicketPdfInfo: WeightTicketPdfInfo,
  weightTicketId: string
): void => {
  const link = document.createElement('a');
  link.href = weightTicketPdfInfo.downloadUrl;
  link.download = `weegbon-${weightTicketId}-${weightTicketPdfInfo.fileName}`;
  link.target = '_blank';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
};

/**
 * Downloads a weight ticket PDF directly from Supabase storage using the storage path
 * @param pdfUrl - The storage path of the weight ticket PDF (e.g., "123/weegbon.pdf")
 * @param fileName - The filename to use for the downloaded file
 * @throws Error if there's an issue downloading the weight ticket PDF
 */
export const downloadWeightTicketPdfDirect = async (
  pdfUrl: string,
  fileName: string
): Promise<void> => {
  const { data: pdfData, error } = await supabase.storage
    .from('weight-tickets')
    .download(pdfUrl);

  if (error) {
    console.error('Error downloading weight ticket PDF:', error);
    throw new Error('Fout bij het downloaden van weegbon PDF');
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
