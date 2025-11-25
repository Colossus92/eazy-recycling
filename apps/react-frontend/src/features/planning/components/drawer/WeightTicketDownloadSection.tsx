import { useState, useEffect } from 'react';
import FilePdf from '@/assets/icons/FilePdf.svg?react';
import {
  fetchWeightTicketPdfInfo,
  downloadWeightTicketPdf,
  WeightTicketPdfInfo,
} from '@/api/services/weightTicketPdfService';

interface WeightTicketDownloadSectionProps {
  weightTicketId?: number;
}

export const WeightTicketDownloadSection = ({
  weightTicketId,
}: WeightTicketDownloadSectionProps) => {
  const [isLoading, setIsLoading] = useState(true);
  const [isDownloading, setIsDownloading] = useState(false);
  const [weightTicketPdfInfo, setWeightTicketPdfInfo] = useState<WeightTicketPdfInfo | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const loadWeightTicketPdfInfo = async () => {
      if (!weightTicketId) {
        setIsLoading(false);
        return;
      }

      setIsLoading(true);
      setError(null);

      try {
        const info = await fetchWeightTicketPdfInfo(weightTicketId.toString());
        setWeightTicketPdfInfo(info);
      } catch (error: any) {
        setError(error.message || 'Onverwachte fout bij het ophalen van weegbon informatie');
      } finally {
        setIsLoading(false);
      }
    };

    loadWeightTicketPdfInfo();
  }, [weightTicketId]);

  const handleDirectDownload = () => {
    if (!weightTicketPdfInfo || isDownloading || !weightTicketId) return;

    setIsDownloading(true);
    downloadWeightTicketPdf(weightTicketPdfInfo, weightTicketId.toString());

    // Reset downloading state after a short delay
    setTimeout(() => setIsDownloading(false), 1000);
  };
  
  return (
    isLoading ? (
      <span className={'text-body-2 text-color-text-secondary'}>
        Weegbon laden...
      </span>
    ) : error ? (
      <span className={'text-body-2 text-red-600'}>{error}</span>
    ) : !weightTicketPdfInfo ? <span className={'text-body-2 text-color-text-secondary '}>
        Geen weegbon beschikbaar
      </span> : (
      <button
        onClick={handleDirectDownload}
        disabled={isDownloading}
        className="flex items-center self-stretch flex-1 gap-2 py-2 pl-2 pr-3 border border-solid border-color-border-primary rounded-radius-md bg-color-surface-primary hover:bg-color-surface-secondary disabled:cursor-not-allowed transition-colors"
        data-testid="weight-ticket-download-button"
      >
        <div className="flex size-10 justify-center items-center border border-solid border-color-border-primary rounded-radius-sm bg-color-surface-secondary">
          <FilePdf className={'size-[25px] text-color-text-secondary'} />
        </div>
        <div className="flex flex-col items-start gap-1 flex-1">
          <div className="flex items-center justify-between self-stretch">
            <span className={'text-subtitle-2 text-color-text-primary'}>
              Weegbon
            </span>
            <span className={'text-body-2 text-color-text-secondary'}>
              {weightTicketPdfInfo.timestamp}
            </span>
          </div>
          <div className="flex items-center justify-between self-stretch">
            <span className={'text-body-2 text-color-text-secondary'}>
              {isDownloading ? 'Weegbon downloaden...' : `${weightTicketPdfInfo.fileSizeKb} KB`}
            </span>
          </div>
        </div>
      </button>
    )
  );
};