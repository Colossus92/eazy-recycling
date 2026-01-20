import { supabase } from '@/api/supabaseClient';
import { format } from 'date-fns';

export interface WaybillInfo {
  fileName: string;
  filePath: string;
  downloadUrl: string;
  timestamp: string;
  fileSizeKb: number;
}

/**
 * Fetches the latest waybill information for a given transport ID from Supabase storage
 * @param transportId - The ID of the transport
 * @returns WaybillInfo object with file details or null if no waybill found
 * @throws Error if there's an issue fetching the waybill
 */
export const fetchWaybillInfo = async (
  transportId: string
): Promise<WaybillInfo | null> => {
  try {
    const { data: files, error: listError } = await supabase.storage
      .from('waybills')
      .list(`waybills/${transportId}`, {
        sortBy: { column: 'created_at', order: 'desc' },
      });

    if (listError) {
      console.error('Error listing waybill files:', listError);
      throw new Error('Fout bij het ophalen van waybill bestanden');
    }

    if (!files || files.length === 0) {
      return null;
    }

    const latestFile = files[0];
    const filePath = `waybills/${transportId}/${latestFile.name}`;

    const { data: signedUrlData, error: urlError } = await supabase.storage
      .from('waybills')
      .createSignedUrl(filePath, 3600); // 1 hour expiry

    if (urlError) {
      console.error('Error creating signed URL:', urlError);
      throw new Error('Fout bij het genereren van download link');
    }

    return {
      timestamp: format(new Date(latestFile.updated_at), 'dd-MM-yyyy HH:mm'),
      fileSizeKb: latestFile.metadata?.size
        ? Math.round(latestFile.metadata.size / 1024)
        : 0,
      fileName: latestFile.name,
      filePath,
      downloadUrl: signedUrlData.signedUrl,
    };
  } catch (error) {
    console.error('Unexpected error fetching waybill info:', error);
    throw error;
  }
};

/**
 * Triggers a download of the waybill file
 * @param waybillInfo - The waybill information containing the download URL
 * @param transportId - The ID of the transport (used in filename)
 */
export const downloadWaybill = (
  waybillInfo: WaybillInfo,
  transportId: string
): void => {
  const link = document.createElement('a');
  link.href = waybillInfo.downloadUrl;
  link.download = `waybill-${transportId}-${waybillInfo.fileName}`;
  link.target = '_blank';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
};
